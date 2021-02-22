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

from .config import configs


class ZeroLengthException(Exception):
    def __init__(self):
        super(Exception, self).__init__()

    def __str__(self):
        return '字符串长度为0.'


class MaxLengthException(Exception):
    def __init__(self, string_length):
        super(Exception, self).__init__()
        self.string_length = string_length

    def __str__(self):
        return '字符串长度：%d, 超过%d个字符限制.' % (self.string_length, configs['tokenizer_limit']['max_string_length'])


class MaxBatchException(Exception):
    def __init__(self, batch_size):
        super(Exception, self).__init__()
        self.batch_size = batch_size

    def __str__(self):
        return '批处理大小：%d, 超过%d批处理限制.' % (self.batch_size, configs['tokenizer_limit']['max_batch_size'])


class UnSupportedException(Exception):
    def __init__(self):
        super(Exception, self).__init__()

    def __str__(self):
        return '输入参数异常.'


class ThreadNumberException(Exception):
    def __init__(self):
        super(Exception, self).__init__()

    def __str__(self):
        return '多进程数必须大于等于1'
