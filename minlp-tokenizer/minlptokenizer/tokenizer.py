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
from minlptokenizer.config import configs
from minlptokenizer.crf_viterbi import CRFViterbi
from minlptokenizer.lexicon import Lexicon
from minlptokenizer.vocab import Vocab
from minlptokenizer.tag import Tag
from minlptokenizer.exception import MaxLengthException, MaxBatchException, ZeroLengthException, UnSupportedException

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
pwd = os.path.dirname(__file__)


class MiNLPTokenizer:

    def __init__(self, file_or_list=None, granularity='fine'):
        """
        分词器初始化
        :param file_or_list: 用户自定义词典文件或列表
        :param granularity: 分词粒度参数，fine表示细粒度分词，coarse表示粗粒度分词
        """
        self.char_dict_path = os.path.join(pwd, configs['vocab_path'])
        self.pb_model_path = os.path.join(pwd, configs['tokenizer_granularity'][granularity]['model'])
        self.trans_path = os.path.join(pwd, configs['tokenizer_granularity'][granularity]['trans'])

        self.vocab = Vocab(self.char_dict_path)
        self.lexicon = Lexicon(file_or_list)
        self.crf = CRFViterbi(self.trans_path)

        with tf.io.gfile.GFile(self.pb_model_path, 'rb') as f:
            graph_def = tf.compat.v1.GraphDef()
            graph_def.ParseFromString(f.read())

        g = tf.Graph()
        with g.as_default():
            tf.import_graph_def(graph_def, name='')
        self.sess = tf.compat.v1.Session(graph=g)
        self.char_ids_input = self.sess.graph.get_tensor_by_name('char_ids_batch:0')
        self.y_logits = self.sess.graph.get_tensor_by_name('logits:0')
        for lexicon_file in configs['lexicon_files']:
            self.lexicon.add_words(os.path.join(pwd, lexicon_file))

    def format_string(self, ustring):
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

            texts = list(map(self.format_string, text_batch))
            feed_dict = {
                self.char_ids_input: self.vocab.get_char_ids(texts),
            }

            y_logits = self.sess.run(self.y_logits, feed_dict=feed_dict)
            y_logits = list(map(self.lexicon.parse_unary_score, texts, y_logits))
            y_pred_results = map(self.crf.viterbi, y_logits)
            return list(map(lambda x, y: self.tag2words(x, y), texts, y_pred_results))
        else:
            raise UnSupportedException()

    def cut(self, text):
        """
        对单条文本进行分词操作
        :param text: 待分词文本
        :return: 分词结果
        """
        if isinstance(text, str):
            text = self.format_string(text)
            feed_dict = {self.char_ids_input: self.vocab.get_char_ids([text])}
            y_logits = self.sess.run(self.y_logits, feed_dict=feed_dict)
            y_logit = self.lexicon.parse_unary_score(text, y_logits[0])
            y_pred_result = self.crf.viterbi(y_logit)
            return self.tag2words(text, y_pred_result)
        else:
            raise UnSupportedException()

    def tag2words(self, text, y_pred_result):
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
        return words

    def set_interfere_factor(self, interfere_factor):
        """
        设置用户词典干预强度，值越大，分词结果越符合词典
        :param interfere_factor: 干预强度，默认值：2
       """
        self.lexicon.set_interfere_factor(interfere_factor)

    def reset_interfere_factor(self):
        """
        重置用户词典干预强度为默认值：2
        """
        self.lexicon.reset_interfere_factor()

    def destroy(self):
        self.sess.close()
