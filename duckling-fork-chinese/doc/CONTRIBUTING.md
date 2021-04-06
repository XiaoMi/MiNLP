# 如何参与

欢迎大家积极踊跃贡献自己宝贵的知识，方便更多的开发者。
在进行Contribute时请确保：

- 首先参考1中的格式提交issue。
- 您的代码遵守现有代码风格的格式约定，可参考2中的方法。
- 对您所做的更改，尽量添加注释以便更快捷的理解修改的内容。
- 如果要引入新功能，请先描述您的想法和例子，在开发完成后确保通过所有的测试用例（CI的BUILD必须是成功）。
- 您的修改应保证测试覆盖率不会降低、且benchmark的性能测试不会明显降低。
- 如果是对现有bug的修正，单元测试中应包括重现问题的用例。
- 不要在代码中出现私人信息。
- 请在提交merge request时，对多个commit进行合并。并为不相关的功能分别提交请求，较小的修改合在一起提交也是可以的。

## 1. 提交Issue

Issue可以是BUG上报，也可以是请求新功能开发的申请，或者用法的提问。提交issue时请标注问题的类别，以便对该Issue的进行分类，Issue的标题可参考下面的格式：

- [DUCKLING-FIX] 现有功能的bug进行修复
- [DUCKLING-IMP] 对现有的功能进行改进
- [DUCKLING-FEATURE] 新功能开发
- [DUCKLING-DISCUSSION] 提问、讨论

提交代码之前可以先发起一个Issue，让人了解你的问题和基本想法。

## 2. 代码风格

Intellij IDEA的设置中打开`Editor -> Code Style -> Scala`，点击Scheme右侧的小按钮，`Import Scheme…`，选择本项目下的`intellij_formating.xml`。

## 3. Git工作流

> 下面是项目的单库多远端的操作，贡献代码前先了解了解。

1. 派生(fork) [MiNLP](https://github.com/XiaoMi/MiNLP) 到自己git 仓库

2. clone fork的git 仓库

```
git clone git@git.n.xiaomi.com/<your name>/duckling-fork-chinese.git
```

3. 添加上游仓库到remote里

```
git remote add upstream https://github.com/XiaoMi/MiNLP.git
```

如果已经建立关系，可以用fetch拉到最新修改

```
git fetch upstream main
```

4. 创建自己的dev分支进行开发

```
git checkout -b feature-xxx remotes/upstream/main
```

5. 在提交一个pull request时rebase自己的修改

```
git pull --rebase upstream main
```

6. 提交代码前，将多次commit合并为一个（GitLab有自动rebase的功能时可以忽略）

   > 已经Review并且有comment的代码尽量不要rebase，避免对后续Review造成困扰。

```
git rebase -i upstream/main
```

7. 提交merge request

```
git push origin feature-xxx
```

提交后对本次修改添加或更新标题，并在内容中描述改动的内容。

8. 冲突解决
   如果在拉取master代码时与自己修改发生冲突，将master更新merge到自己分支，并解决冲突，重新push

```
git merge upstream/main
git push origin feature-xxx
```