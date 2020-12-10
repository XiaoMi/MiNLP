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
from time import time
import tensorflow as tf
from minlptokenizer.tokenizer import MiNLPTokenizer

class TestSpeed(unittest.TestCase):

    def setUp(self):
        self.file_size = os.path.getsize('speed_test.txt') / 1024.0
        self.file=open('E:\icwb2-data\scripts\speed_test.txt','r',encoding='utf-8')
        self.case_list=[sentence for sentence in self.file]

    def test_cut_speed(self):
        tokenizer = MiNLPTokenizer(granularity='fine')
        start_time = time()
        for i in self.case_list:
            tokenizer.cut(i)
        end_time = time()
        print('cut_cost :', end_time - start_time)
        print('cut_speed: ', self.file_size / (end_time - start_time))

    def test_cut_multi_speed(self):
        tf_config = tf.compat.v1.ConfigProto()
        tf_config.gpu_options.allow_growth = True
        start_time = time()
        MiNLPTokenizer.set_memmap_folder('.')
        MiNLPTokenizer.cut_batch_multiprocess(self.case_list, file_or_list=None, granularity='fine', n_jobs=4)
        end_time = time()
        print('multi_cut_cost :', end_time - start_time)
        print('multi_cut_speed: ', self.file_size / (end_time - start_time))


if __name__ == '__main__':
    unittest.main()

