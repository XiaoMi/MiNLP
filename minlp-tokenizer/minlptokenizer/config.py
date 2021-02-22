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


configs = {
    'vocab_path': 'vocab/b.14067n.300d.vocab',
    'tokenizer_granularity': {
        'fine': {
            'model': 'model/zh/b-fine-cnn-crf-an2cn.pb',
        },
        'coarse': {
            'model': 'model/zh/b-coarse-cnn-crf-an2cn.pb',
        }
    },
    'tokenizer_limit': {
        'max_batch_size': 128,
        'max_string_length': 1024
    },
    'lexicon_files': [
        'lexicon/default.txt',
        'lexicon/chengyu.txt',
    ]
}
