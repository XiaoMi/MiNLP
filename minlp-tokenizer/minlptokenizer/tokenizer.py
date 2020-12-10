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
import tempfile
import tensorflow as tf
import itertools
from joblib import Parallel, delayed
from itertools import chain
from functools import partial
from minlptokenizer.config import configs
from minlptokenizer.crf_viterbi import CRFViterbi
from minlptokenizer.lexicon import Lexicon
from minlptokenizer.vocab import Vocab
from minlptokenizer.tag import Tag
from minlptokenizer.exception import MaxLengthException, ZeroLengthException, UnSupportedException, MaxBatchException, \
    FolderException

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
pwd = os.path.dirname(__file__)


def minibatch(items, size=8):
    """Iterate over batches of items. `size` may be an iterator,
    so that batch-size can vary on each step.
    """
    if isinstance(size, int):
        size_ = itertools.repeat(size)
    else:
        size_ = size
    items = iter(items)
    while True:
        batch_size = next(size_)
        batch = list(itertools.islice(items, int(batch_size)))
        if len(batch) == 0:
            break
        yield list(batch)


class MiNLPTokenizer:
    tokenizer_singleton = None
    temp_folder = tempfile.gettempdir()

    def __init__(self, file_or_list=None, granularity='fine', tf_config=None):
        """
        分词器初始化
        :param file_or_list: 用户自定义词典文件或列表
        :param granularity: 分词粒度参数，fine表示细粒度分词，coarse表示粗粒度分词
        """
        self.__char_dict_path = os.path.join(pwd, configs['vocab_path'])
        self.__pb_model_path = os.path.join(pwd, configs['tokenizer_granularity'][granularity]['model'])
        self.__trans_path = os.path.join(pwd, configs['tokenizer_granularity'][granularity]['trans'])
        self.__vocab = Vocab(self.__char_dict_path)
        self.__lexicon = Lexicon(file_or_list)
        self.__crf = CRFViterbi(self.__trans_path)

        with tf.io.gfile.GFile(self.__pb_model_path, 'rb') as f:
            graph_def = tf.compat.v1.GraphDef()
            graph_def.ParseFromString(f.read())

        g = tf.Graph()
        with g.as_default():
            tf.import_graph_def(graph_def, name='')
        if tf_config:
            self.__sess = tf.compat.v1.Session(graph=g, config=tf_config)
        else:
            self.__sess = tf.compat.v1.Session(graph=g)
        self.__char_ids_input = self.__sess.graph.get_tensor_by_name('char_ids_batch:0')
        self.__y_logits = self.__sess.graph.get_tensor_by_name('logits:0')
        for lexicon_file in configs['lexicon_files']:
            self.__lexicon.add_words(os.path.join(pwd, lexicon_file))

    def __format_string(self, ustring):
        """
        全角转半角，多个连续控制符、空格替换成单个空格
        """

        if not ustring.strip():
            raise ZeroLengthException()

        rstring = ""
        for uchar in ustring:
            inside_code = ord(uchar)
            if inside_code == 12288:  # 全角空格直接转换
                inside_code = 32
            elif 65281 <= inside_code <= 65374:  # 全角字符（除空格）转化
                inside_code -= 65248

            rstring += chr(inside_code)
        if len(rstring) > configs['tokenizer_limit']['max_string_length']:
            raise MaxLengthException(len(rstring))
        return regex.sub(r'[\p{Z}\s]+', ' ', rstring.strip())

    def cut_batch(self, text_batch):
        """
        对批量文本进行分词
        :param text_batch: 待分词文本列表
        :return: 分词结果列表
       """
        if isinstance(text_batch, list):
            if len(text_batch) > configs['tokenizer_limit']['max_batch_size']:
                raise MaxBatchException(len(text_batch))

            texts = list(map(self.__format_string, text_batch))
            feed_dict = {
                self.__char_ids_input: self.__vocab.get_char_ids(texts),
            }

            y_logits = self.__sess.run(self.__y_logits, feed_dict=feed_dict)
            y_logits = list(map(self.__lexicon.parse_unary_score, texts, y_logits))
            y_pred_results = map(self.__crf.viterbi, y_logits)
            return list(map(lambda x, y: self.__tag2words(x, y), texts, y_pred_results))
        else:
            raise UnSupportedException()

    @staticmethod
    def cut_batch_in_one_process(cls, file_or_list, granularity, text_batch):
        if cls.tokenizer_singleton is None:
            tf_config = tf.compat.v1.ConfigProto()
            tf_config.gpu_options.allow_growth = True
            cls.tokenizer_singleton = cls(file_or_list, granularity, tf_config=tf_config)
        return cls.tokenizer_singleton.cut_batch(text_batch)

    @classmethod
    def cut_batch_multiprocess(cls, text_batch, file_or_list=None, granularity='fine', n_jobs=2):
        """
        使用多线程对多条本文进行分词操作，仅仅在GPU情况下有效
        CPU场景下，TF会默认使用所用核心
        """
        partitions = minibatch(text_batch, size=configs['tokenizer_limit']['max_batch_size'])
        executor = Parallel(n_jobs=n_jobs, backend="multiprocessing", prefer="processes", temp_folder=cls.temp_folder)
        do = delayed(partial(cls.cut_batch_in_one_process, cls, file_or_list, granularity))
        tasks = (do(batch) for i, batch in enumerate(partitions))
        try:
            res = list(chain(*executor(tasks)))
        except UnicodeEncodeError:
            raise FolderException
        return res

    def cut(self, text):
        """
        对单条文本进行分词操作
        :param text: 待分词文本
        :return: 分词结果
        """
        if isinstance(text, str):
            text = self.__format_string(text)
            feed_dict = {self.__char_ids_input: self.__vocab.get_char_ids([text])}
            y_logits = self.__sess.run(self.__y_logits, feed_dict=feed_dict)
            y_logit = self.__lexicon.parse_unary_score(text, y_logits[0])
            y_pred_result = self.__crf.viterbi(y_logit)
            return self.__tag2words(text, y_pred_result)
        else:
            raise UnSupportedException()

    def __tag2words(self, text, y_pred_result):
        words = []
        word = ''
        for idx, ch in enumerate(text):
            word += ch
            tag = y_pred_result[idx]
            if tag == Tag.S.value or tag == Tag.E.value or tag == Tag.X.value:
                words.append(word)
                word = ''
        if word:
            words.append(word)
        return regex.split(r'\s+', ' '.join(words))

    @classmethod
    def set_memmap_folder(cls, path):
        """
        设置memap的路径（cut_batch_multiprocess 时会使用内存映射来进行通信）
        由于Joblib本身编码原因。路径不要含有中文字符
        :param path: memap路径
        :return:
        """
        cls.temp_folder = path

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

    def destroy(self):
        self.__sess.close()
