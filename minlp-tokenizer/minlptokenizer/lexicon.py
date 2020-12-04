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

import ahocorasick
from collections import Iterable
import numpy as np
from minlptokenizer.tag import Tag

DEFAULT_INTERFERE_FACTOR = 2


class Lexicon:
    lexicon = None

    def __init__(self, file_or_list=None):
        self.ac = ahocorasick.Automaton(ahocorasick.STORE_LENGTH)
        if file_or_list:
            self.add_words(file_or_list)
        self.interfere_factor = DEFAULT_INTERFERE_FACTOR

    def add_word(self, word):
        """
        添加干预词到用户词典中
        :param word: 干预词
        :return:
        """
        self.ac.add_word(word)

    def add_words(self, file_or_list):
        """
        添加干预文件或者词列表
        :param file_or_list: 文件路径或者词列表
        :return:
        """
        if isinstance(file_or_list, str):  # param is a filename
            with open(file_or_list, encoding="UTF-8") as fin:
                lines = fin.read().splitlines()
                self.add_words(lines)
        elif isinstance(file_or_list, Iterable):  # param is a iterable type
            for word in filter(lambda t: t and not t.startswith('#'), file_or_list):
                self.ac.add_word(word)

    def product_factor(self, texts):
        """
        根据用户词典生成句子对应的干预权重矩阵
        :param texts: 目标句子
        :return: 干预权重矩阵
        """
        if self.ac.kind is not ahocorasick.AHOCORASICK:
            self.ac.make_automaton()
        max_len = max(map(len, texts))
        matrix = []
        for text in texts:
            factor_matrix = np.ones(shape=[max_len, Tag.__len__()])
            for (end_pos, length) in self.ac.iter(text):
                start_pos = end_pos - length + 1
                if length == 1:
                    factor_matrix[start_pos][1] *= self.interfere_factor
                else:
                    factor_matrix[start_pos][2] *= self.interfere_factor
                    factor_matrix[end_pos][4] *= self.interfere_factor
                    for i in range(start_pos + 1, end_pos):
                        factor_matrix[i][3] *= self.interfere_factor
            matrix.append(factor_matrix)
        return matrix

    def max_socre(self, scores):
        return self.interfere_factor * abs(max(scores))

    def set_interfere_factor(self, interfere_factor):
        """
        设置干预权重
        :param interfere_factor:干预权重因子
        :return:
        """
        self.interfere_factor = interfere_factor

    def reset_interfere_factor(self):
        """
        重置干预权重
        :return:
        """
        self.interfere_factor = DEFAULT_INTERFERE_FACTOR
