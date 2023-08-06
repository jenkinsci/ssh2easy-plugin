# ssh2easy

This plugin allows you to SSH2 remote server to execute Linux commands, shell, SFTP upload, download etc.

## Quick Introduction

I ever worked at cloud system CI related tasks. Till now, there are several SSH plugins. But they didn't fulfill our team's requirements towards its management design and function. Based on that, I redesign and rebuild this new plugin during my daily work. As time went by, this plugin always provide good solution and resolve SSH requirements almost compared with others. Its main advantages include:

1. In a cloud-based large scale company, its development environment and testing environment is so complicated. Its application deploy mode is multi-styles based on different product. Some of others SSH plugin can't work on such as nohup start related features. When its SSH2 input stream and output stream is keep alive, their build process will hung on etc. This plugin - SSH2Easy - can resolve this problem.
2. When there are so many servers need to management by Jenkins, we will need to input so many redundant SSH info, such as SSH port, SSH username, password with the other SSH plugins. Actually, the most of VM and servers have the same information for these items. Just only its host IP is different. So if you use this plugin - SSH2Easy - it is more convenient to manage them by server group mode.
3. This plugin also provide "Disable" feature for its build steps. During daily work, this feature is also very convenient.

Overall, this plugin want to provide very simple and easy ways for its useful features of SSH2 related features.
So I want to share it on public Jenkins Update Center here.
Hope helpful to some of us towards on SSH2 related requirements.

### 1. SSH2 accounts group management

![](docs/images/1.png)
![](docs/images/2.png)

### 2. SSH2 remote shell and command

![](docs/images/3.png)

### 3. SFTP remote upload and download

![](docs/images/4.png)
