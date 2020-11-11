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

import unittest
import os
from minlptokenizer.vocab import Vocab
from minlptokenizer.config import configs


class TestVocab(unittest.TestCase):
    """
    字符转ID测试
    """
    def setUp(self):
        src_dir = os.path.join(os.path.dirname(__file__), '../minlptokenizer')
        self.vocab = Vocab(os.path.join(src_dir, configs['vocab_path']))

    def test_get_char_ids(self):
        s = ['让全球每个人都能享受科技带来的美好生活', '科技带来的美好生活', '美好生活']
        self.assertListEqual(
            self.vocab.get_char_ids(s),
            [
                [568, 117, 442, 464, 115, 5, 187, 106, 1456, 363, 88, 353, 357, 66, 8, 150, 193, 21, 423],
                [88, 353, 357, 66, 8, 150, 193, 21, 423, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                [150, 193, 21, 423, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
            ]
        )


if __name__ == '__main__':
    unittest.main()
