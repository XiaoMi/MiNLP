# Copyright 2020 The MiNLP Authors. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import regex
import os
import tensorflow as tf
import math
from minlptokenizer.config import configs
from minlptokenizer.lexicon import Lexicon
from minlptokenizer.vocab import Vocab
from minlptokenizer.tag import Tag
from minlptokenizer.exception import MaxLengthException, ZeroLengthException, UnSupportedException, MaxBatchException
from multiprocessing import Pool, cpu_count

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
pwd = os.path.dirname(__file__)


def batch_generator(list_texts, size=configs['tokenizer_limit']['max_batch_size']):
    """
    list generator 用于迭代生成batch
    :param list_texts:待切分的语料列表
    :param size: 每个batch的大小
    :return: 迭代器
    """
    if isinstance(list_texts, list) or isinstance(list_texts, tuple):
        batch_num = math.ceil(len(list_texts) / size)
        for i in range(batch_num):
            yield list_texts[i * size:(i + 1) * size]


def format_string(ustring):
    """
    全角转半角，多个连续控制符、空格替换成单个空格
    """
    if not ustring.strip():
        raise ZeroLengthException()

    if len(ustring) > configs['tokenizer_limit']['max_string_length']:
        raise MaxLengthException(len(ustring))

    half_wide_string = ""
    for uchar in ustring:
        inside_code = ord(uchar)
        if inside_code == 12288:  # 全角空格直接转换
            inside_code = 32
        elif 65281 <= inside_code <= 65374:  # 全角字符（除空格）转化
            inside_code -= 65248
        half_wide_string += chr(inside_code)

    return regex.sub(r'[\p{Z}\s]+', ' ', half_wide_string.strip())


def tag2words(text, predict_results):
    words = []
    word = ''
    for idx, ch in enumerate(text):
        word += ch
        tag = predict_results[idx]
        if tag == Tag.S.value or tag == Tag.E.value or tag == Tag.X.value:
            words.append(word)
            word = ''
    if word:
        words.append(word)
    return regex.split(r'\s+', ' '.join(words))


class MiNLPTokenizer:
    sess_dict = {'fine': None, 'coarse': None}

    def __init__(self, file_or_list=None, granularity='fine'):
        """
        分词器初始化
        :param file_or_list: 用户自定义词典文件或列表
        :param granularity: 分词粒度参数，fine表示细粒度分词，coarse表示粗粒度分词
        """
        self.__vocab_path = os.path.join(pwd, configs['vocab_path'])
        self.__pb_model_path = os.path.join(pwd, configs['tokenizer_granularity'][granularity]['model'])
        self.__vocab = Vocab(self.__vocab_path)
        self.__lexicon = Lexicon(file_or_list)
        self.__granularity = granularity
        for lexicon_file in configs['lexicon_files']:
            self.__lexicon.add_words(os.path.join(pwd, lexicon_file))

    def __cut(self, text_batch):
        """
        分词函数
        :param text_batch: 待分词字符串列表
        :return: 分词结果
        """
        if not MiNLPTokenizer.sess_dict[self.__granularity]:
            with tf.io.gfile.GFile(self.__pb_model_path, 'rb') as f:
                graph_def = tf.compat.v1.GraphDef()
                graph_def.ParseFromString(f.read())
            g = tf.Graph()
            with g.as_default():
                tf.import_graph_def(graph_def, name='')
            tf_config = tf.compat.v1.ConfigProto()
            tf_config.gpu_options.allow_growth = True  # 使用过程中动态申请显存，按需分配
            MiNLPTokenizer.sess_dict[self.__granularity] = tf.compat.v1.Session(graph=g, config=tf_config)

        sess = MiNLPTokenizer.sess_dict[self.__granularity]
        char_ids_input = sess.graph.get_tensor_by_name('char_ids_batch:0')
        factor_input = sess.graph.get_tensor_by_name('factor_batch:0')
        tag_ids = sess.graph.get_tensor_by_name('tag_ids:0')

        texts = list(map(format_string, text_batch))

        factor = self.__lexicon.product_factor(texts)
        input_char_id = self.__vocab.get_char_ids(texts)
        feed_dict = {
            char_ids_input: input_char_id,
            factor_input: factor
        }
        predict_results = sess.run(tag_ids, feed_dict=feed_dict)
        return list(map(lambda x, y: tag2words(x, y), texts, predict_results))

    def cut(self, text_or_list, n_jobs=cpu_count()):
        """
        分词函数，支持传入字符串或者字符串列表
        :param text_or_list: 待分词字符串或者字符串列表
        :param n_jobs: 进程数量，默认为核心数
        :return: 分词结果
        """
        if isinstance(text_or_list, str):
            return self.__cut([text_or_list])[0]
        elif isinstance(text_or_list, list):
            # generator = batch_generator(text_or_list, size=configs['tokenizer_limit']['max_batch_size'])
            # return [self.__cut(batch) for batch in generator]

            generator = batch_generator(text_or_list, size=configs['tokenizer_limit']['max_batch_size'])
            process_pool = Pool(n_jobs)
            return process_pool.map(self.cut, generator)
        else:
            raise UnSupportedException()

    def set_interfere_factor(self, interfere_factor):
        """
        设置用户词典干预强度，值越大，分词结果越符合词典
        :param interfere_factor: 干预强度，默认值：2
       """
        self.__lexicon.set_interfere_factor(interfere_factor)

    def reset_interfere_factor(self):
        """
        重置用户词典干预强度为默认值：2
        """
        self.__lexicon.reset_interfere_factor()
