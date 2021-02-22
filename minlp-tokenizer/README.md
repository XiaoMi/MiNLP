# MiNLP-Tokenizer

## 1. 工具介绍

MiNLP-Tokenizer是小米AI实验室NLP团队自研的中文分词工具，基于深度学习序列标注模型实现，在公开测试集上取得了SOTA效果。其具备以下特点：
- **分词效果好**：基于深度学习模型在大规模语料上进行训练，粗、细粒度在SIGHAN 2005 PKU测试集上的F1分别达到95.7%和96.3%[注1]
- **轻量级模型**：精简模型参数和结构，模型仅有20MB
- **词典可定制**：灵活、方便的干预机制，根据用户词典对模型结果进行干预
- **多粒度切分**：提供粗、细粒度两种分词规范，满足各种场景需要
- **调用更便捷**：一键快速安装，API简单易用

注1：我们结合公司应用场景，制定了粗、细粒度分词规范，并按照规范对PKU测试集重新进行了标注（由于测试集版权限制，未包含在本项目中）。

## 2. 安装

pip全自动安装：
```
pip install minlp-tokenizer
```
适用环境：Python 3.5~3.8，TensorFlow>=1.14

## 3. 使用API

- 分词（逐句或者列表）：
```python
from minlptokenizer.tokenizer import MiNLPTokenizer

tokenizer = MiNLPTokenizer(granularity='fine')  # fine：细粒度，coarse：粗粒度，默认为细粒度
print(tokenizer.cut('今天天气怎么样？'))  # 单句分词
# ['今天','天气','怎么样']  
print(tokenizer.cut(['今天天气怎么样', '小米的价值观是真诚与热爱']))  # list分词，list长度小于128
# [['今天','天气','怎么样'],['小米','的','价值观','是','真诚','与','热爱']]
```

- 通过迭代器进行批量分词：
```python
from minlptokenizer.tokenizer import MiNLPTokenizer, batch_generator

texts = ['小米的价值观是真诚与热爱'] * 2048
tokenizer = MiNLPTokenizer(granularity='fine')
batch_iter = batch_generator(texts, size=128)  # MiNLP提供迭代器，也可自行迭代
for batch in batch_iter:
    print(tokenizer.cut(batch))

```

- 多进程批量分词：
```python
from minlptokenizer.tokenizer import MiNLPTokenizer

texts = ['小米的价值观是真诚与热爱'] * 2048
tokenizer = MiNLPTokenizer(granularity='fine')
result = tokenizer.cut_batch_multiprocess(texts, granularity='fine', n_jobs=2)  # n_jobs:分词进程数量(默认为2)
```

- 文件分词(适用于大文件分词)：
```python
from minlptokenizer.tokenizer import MiNLPTokenizer

tokenizer = MiNLPTokenizer(granularity='fine')
tokenizer.cut_from_file(file_path='/path/to/your/file', # file_path:待分词文件，每行一句
                        save_path='path/to/result', # save_path:分词结果保存位置
                        n_jobs=2)  # n_jobs:进程数
```
## 4. 自定义用户词典

- List添加/文件路径方式：
 ```python
from minlptokenizer.tokenizer import MiNLPTokenizer

tokenizer = MiNLPTokenizer(file_or_list=['word1', 'word2'], granularity='fine')  # 用户自定义干预词典传入
tokenizer = MiNLPTokenizer(file_or_list='/path/to/your/lexicon/file', granularity='coarse')  # 构造函数的参数为用户词典路径
 ```
 
## 5. 注意事项
由于Windows环境下Python multiprocessing和Linux不同，Linux基于Fork实现多进程，Windows则是启动新进程。因此，在Windows环境下使用多进程分词(cut_batch_multiprocess)
或文件分词(cut_from_file)时，请务必保证调用时在 if \_\_name__=='\_\_main__'的保护区域内，例如：
```python
from minlptokenizer.tokenizer import MiNLPTokenizer

# Windows 环境下使用多进程分词
if __name__ == '__main__':
    texts = ['小米的价值观是真诚与热爱'] * 2048
    tokenizer = MiNLPTokenizer(granularity='fine')
    result = tokenizer.cut_batch_multiprocess(texts, granularity='fine', n_jobs=2)
    tokenizer.cut_from_file(file_path='/path/to/your/file', save_path='path/to/result', n_jobs=2)
```

## 6. 未来计划

MiNLP是小米AI实验室NLP团队开发的小米自然语言处理平台，目前已经具备词法、句法、语义等数十个功能模块，在公司业务中得到了广泛应用。
第一阶段我们开源了MiNLP的中文分词功能，后续我们将陆续开源词性标注、命名实体识别、句法分析等功能，和开发者一起打造功能强大、效果领先的NLP工具集。

## 7. 参与开发

我们欢迎开发者向MiNLP-Tokenizer贡献代码，也欢迎提出各种Issue和反馈意见。
开发流程详见CONTRIBUTING.md。

## 8. 开发者致谢

感谢社区众多的开发者对MiNLP-Tokenizer提出的支持、意见、鼓励和建议。在此特别感谢以下开发者为MiNLP-Tokenizer分词工具贡献了PR：
 - 2020.12.4  aseaday 贡献了有关批量分词的速度优化代码，在V100/RTX TITAN的环境下，批量分词速度由40-50KB/s提升至140-150KB/s。

## 9. 在学术成果中使用

如果您在学术成果中使用了MiNLP中文分词工具，请按如下格式引用：
  - 中文：郭元凯, 史亮, 陈宇鹏, 孟二利, 王斌. MiNLP-Tokenizer：小米中文分词工具. 2020.
  - 英文：Yuankai Guo, Liang Shi, Yupeng Chen, Erli Meng, Bin Wang. MiNLP-Tokenzier: XiaoMi Chinese Word Segmenter. 2020.

