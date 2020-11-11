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

import codecs


class Vocab:
    def __init__(self, vocab_path):
        """
        分词词典完成char->id映射
        :param vocab_path:词典路径
        """
        self.vocab_map = {}
        with codecs.open(vocab_path, encoding='utf-8') as fin:
            for idx, key in enumerate(fin):
                self.vocab_map[key.strip()] = idx
        self.oovCharID = self.vocab_map.get('[OOV]')
        self.padCharID = self.vocab_map.get('[PAD]')

    def get_char_ids(self, text_list):
        """
        获取文本对应charid
        :param text_list:文本集合
        :return:id集合
        """
        max_len = max(map(len, text_list))
        return list(map(
            lambda x: [self.vocab_map.get(c, self.oovCharID) for c in x] + [self.padCharID] * (max_len - len(x)),
            text_list
        ))
