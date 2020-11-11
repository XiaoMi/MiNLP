# 如何参与

欢迎大家对MiNLP-tokenizer贡献自己宝贵的知识，方便更多的开发者。
在进行Contribute时请确保：

- 对您所做的更改，尽量添加注释以便更快捷的理解修改的内容。
- 如果要引入新功能，请在pull request中描述您的想法和例子，在开发完成后确保通过所有的测试用例。
- 您的修改应保证在SIGHAN 2005 PKU测试集上进行测试并记录F1值。
- 如果是对现有bug的修正，单元测试中应包括重现问题的用例。
- 不要在代码中出现私人信息。
- 请在提交pull request时，对多个commit进行合并。并为不相关的功能分别提交请求，较小的修改合在一起提交也是可以的。


## 1. SIGHAN 2005 PKU测试集

下载[icwb2-data](http://sighan.cs.uchicago.edu/bakeoff2005/)，可以使用SIGHAN-PKU测试集进行分词测试。
注：我们结合公司应用场景，制定了粗、细粒度分词规范，并按照规范对PKU测试集重新进行了标注（由于测试集版权限制，未包含在本项目中）。
由于分词标准不一致，因此使用SIGHAN-PKU官方测试集的评价结果可能有所降低。

## 2. GitHub流程
简单流程如下：

(1) fork [MiNLP-Tokenizer](https://github.com/XiaoMi/MiNLP) 到自己的 git 仓库

```
https://github.com/XiaoMi/MiNLP
```

(2) 从自己的 git 仓库clone

```
git clone git@github.com:<username>/MiNLP.git
```

使用自己的git账号替换<username>


(3) 创建自己的feature分支进行开发

```
git checkout -b feature-xxx remotes/upstream/develop
```

(4) 保持与当前develop分支同步，提交分支
```
git rebase -i upstream/develop
git push origin feature-xxx
```

(5) 从个人副本发起 pull request 并填写一个清楚有效的改动描述

## 3. 分支合并

项目的维护者 guoyuankai@xiaomi.com、shiliang1@xiaomi.com 会对发起的pull request进行review并对合适的代码进行合并，再次感谢各位开发者为项目作出的贡献。
