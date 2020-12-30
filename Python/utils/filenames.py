#!/usr/bin/env python3
# encoding: utf-8
# @author:   hoojo
# @email:    hoojo_@126.com
# @github:   https://github.com/hooj0
# @create:   2020/12/30 0030
# @copyright by hoojo @2018
# @changelog Added  `file names` python example


# ===============================================================================
# 标题：输出给定目录下的所有文件名
# ===============================================================================
# 使用：
# terminal command:
#       python filenames.py dir
#       python filenames.py dir -s txt
#       python filenames.py dir -s txt py
# -------------------------------------------------------------------------------
# 描述：输出给定目录下的所有文件名，可以通过后缀过滤
# -------------------------------------------------------------------------------
import os
import sys
import getopt


# -------------------------------------------------------------------------------
# 完成文件查找函数
# -------------------------------------------------------------------------------
def filenames(suffix, dir="."):
    print("查找目录[%s]下的后缀为[%s]文件列表" % (dir, suffix))

    if not os.path.exists(dir):
        print("目录不存在：", dir)
    else:
        if os.path.isfile(dir):
            file = os.path.basename(dir)
            output(suffix, file)
        else:
            for root, dirs, files in os.walk(dir):
                for item in files:
                    file_path = os.path.join(root, item)
                    output(suffix, file_path)


def output(suffix, file):
    file_suffix = file.split(".")[-1]

    if suffix is None:
        print(file)
        return

    if file_suffix in suffix:
        print(file)


def main(argv):
    # print('argv: %s' % argv)

    help_usage = '''
    USAGE: python filenames.py [OPTIONS] -h --suffix file.suffix [DIR] directory
    
    OPTIONS: 
      -h,--help         use the help manual.
      -s,--suffix       filter file suffix. default: all
      
    COMMANDS:
      help        use the help manual
        
    EXAMPLES: 
      python filenames.py -h
      python filenames.py help
    
      python filenames.py
      python filenames.py /home/ebook/
      
      python filenames.py -s py /home/ebook
      python filenames.py -s py txt /home/ebook
    '''

    # default run current path
    if len(argv) < 1:
        filenames(None)
        sys.exit()

    try:
        long_opts = ["help", "suffix="]
        opts, args = getopt.getopt(argv, "hs:", long_opts)
        # print('opts: %s, args: %s' % (opts, args))
    except getopt.GetoptError:
        print(help_usage)
        sys.exit(2)

    suffix = None
    for opt, arg in opts:
        if opt in ('-h', '--help'):
            print(help_usage)
            sys.exit()
        elif opt in ("-s", "--suffix"):
            suffix = arg

    for arg in args:
        if arg == 'help':
            print(help_usage)
            sys.exit()
        else:
            filenames(suffix, arg)

    if len(args) == 0:
        filenames(suffix)

    if len(args) < 0:
        print(help_usage)
        sys.exit()


if __name__ == "__main__":
    main(sys.argv[1:])
