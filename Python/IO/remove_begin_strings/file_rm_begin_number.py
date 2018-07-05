#!/bin/python3
# author: hoojo

#----------------------------------------------------------------
# 将 endorsement-policies.md.bak 文件中的行号删除，
# 并新建一个文件 endorsement-policies.md 写入删除行号后的文本内容
#----------------------------------------------------------------

contents = []

with open("endorsement-policies.md.bak", "r") as file:        
    while True:
        text = file.readline()

        if (len(text) == 0):
            break
        
        contents.append(text[5:])
        

md = open('endorsement-policies.md', 'w+')
md.writelines(contents)
md.close()
