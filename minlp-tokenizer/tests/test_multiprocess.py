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
from minlptokenizer.tokenizer import MiNLPTokenizer


class TestMultiprocess(unittest.TestCase):

    def setUp(self):
        self.case = '粗细粒度的区别包括三点十分2020年1月1日等。'
        self.case_list = [self.case] * 256

    def test_fine_multiprocess(self):
        tokenizer = MiNLPTokenizer(granularity='fine')
        self.assertListEqual(
            tokenizer.cut(self.case_list, n_jobs=2),
            [['粗细', '粒度', '的', '区别', '包括', '三', '点', '十', '分', '2020', '年', '1', '月', '1', '日', '等', '。']] * 256
        )

    def test_coarse_multiprocess(self):
        tokenizer = MiNLPTokenizer(granularity='coarse')
        self.assertListEqual(
            tokenizer.cut(self.case_list, n_jobs=2),
            [['粗细', '粒度', '的', '区别', '包括', '三点', '十分', '2020年', '1月', '1日', '等', '。']] * 256
        )

    def test_user_dict(self):
        tokenizer = MiNLPTokenizer(file_or_list=['粗细粒度'])
        self.assertListEqual(
            tokenizer.cut(self.case_list, n_jobs=2),
            [['粗细粒度', '的', '区别', '包括', '三', '点', '十', '分', '2020', '年', '1', '月', '1', '日', '等', '。']] * 256
        )


if __name__ == '__main__':
    unittest.main()
