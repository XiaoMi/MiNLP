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

import sys
import numpy as np


class CRFViterbi:

    def __init__(self, trans_path):
        self.trans_score = np.loadtxt(trans_path)

        # [x->x, x->s, x->b, x->m, x->e]
        # [s->x, s->s, s->b, s->m, s->e]
        # [b->x, b->s, b->b, b->m, b->e]
        # [m->x, m->s, m->b, m->m, m->e]
        # [e->x, e->s, e->b, e->m, e->e]

    def viterbi(self, unary_score):
        """
        根据发射概率矩阵和转移概率矩阵进行维特比算法，求出最大概率序列
        :param unary_score:
        :return: 最大概率序列
        """
        max_matrix = np.full_like(
            unary_score,
            sys.float_info.min,
            dtype=np.float)
        path = np.full_like(unary_score, 0, dtype=np.int)
        max_matrix[0] = unary_score[0]
        seq_len, tag_size = unary_score.shape

        # dp-min
        for seq_idx in range(1, seq_len):
            for cur_tag_idx in range(tag_size):
                for prev_tag_idx in range(tag_size):
                    cur_score = max_matrix[seq_idx - 1, prev_tag_idx] + \
                        unary_score[seq_idx, cur_tag_idx] + \
                        self.trans_score[prev_tag_idx, cur_tag_idx]
                    if cur_score > max_matrix[seq_idx, cur_tag_idx]:
                        max_matrix[seq_idx, cur_tag_idx] = cur_score
                        path[seq_idx, cur_tag_idx] = prev_tag_idx

        # find path
        pre_max_index = np.argmax(max_matrix[seq_len - 1])
        min_path = []
        for seq_idx in range(seq_len - 1, -1, -1):
            min_path.append(pre_max_index)
            pre_max_index = path[seq_idx, pre_max_index]
        min_path.reverse()

        return min_path
