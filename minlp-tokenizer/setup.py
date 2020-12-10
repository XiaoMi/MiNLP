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

from setuptools import setup
import pkg_resources

with open('README.md', 'r', encoding='utf-8') as fh:
    long_description = fh.read()
install_requires = ['pyahocorasick', 'regex']
try:
    import tensorflow
    # 若版本过低，则进行版本更新
    installed_packages = pkg_resources.working_set
    tensorflow_pkgs = [i.key for i in installed_packages if 'tensorflow' in i.key]
    if 'tensorflow-gpu' in tensorflow_pkgs:
        install_requires.append('tensorflow-gpu>=1.14')
    else:
        install_requires.append('tensorflow>=1.14')
except ImportError:
    install_requires.append('tensorflow>=1.14')

setup(
    name='minlp-tokenizer',
    version='3.3.3',
    description='MiNLP-Tokenizer中文分词工具',
    author='Yuankai Guo, Liang Shi, Yupeng Chen',
    author_email='guoyuankai@xiaomi.com, shiliang1@xiaomi.com',
    long_description=long_description,
    long_description_content_type='text/markdown',
    url='https://github.com/XiaoMi/MiNLP',
    license='Apache 2.0',
    python_requires='>=3.5, <=3.8',
    packages=['minlptokenizer'],
    package_dir={'minlptokenizer': 'minlptokenizer'},
    package_data={'minlptokenizer': ['model/zh/*', 'vocab/*', 'lexicon/*', 'trans/*']},
    zip_safe=False,
    install_requires=install_requires,
    classifiers=[
        'License :: OSI Approved :: Apache Software License',
        'Programming Language :: Python :: 3',
        'Programming Language :: Python :: 3.5',
        'Programming Language :: Python :: 3.6',
        'Programming Language :: Python :: 3.7',
        'Programming Language :: Python :: 3.8',
        'Operating System :: OS Independent',
    ]
)
