# -- coding: utf-8 --

import time
import hashlib
import random
import requests



# 签名方法说明
# 注册用户需要签名，需要在header中加入nonce，timestamp， sign。
# 值如下所示。
# 服务器会检查时间戳，2个小时内有效。注意测试使用时避免过期
# 请注意需要与wildfirechat.conf中的SECRET_KEY保持一致

#需要用到requests库，首先要安装pip
#sudo easy_install pip

#然后在安装requests module
#sudo pip install requests

import sys

if len(sys.argv) < 4:
    print 'Usage： python register.py username password displayname'
    sys.exit()
    
user = sys.argv[1]
pwd = sys.argv[2]
displayname = sys.argv[3]

SECRET_KEY = '123456'
URL = 'http://localhost:18080/admin/user/create'

ts = (int(round(time.time() * 1000)))
random.seed(time.time())
nonce = (int(round(random.random() * 100000)))
orignSign = bytes(nonce) + '|' + SECRET_KEY + '|' + bytes(ts)
sign = hashlib.sha1(orignSign).hexdigest()

headers = {'nonce': bytes(nonce), 'timestamp':bytes(ts), 'sign':sign, 'Content-Type':'application/json'}

payload = {'name':user, 'password':pwd, 'displayName':displayname}

print 'headers:'
print headers
print
print 'payload:'
print payload
print

r = requests.post(URL, headers = headers, json = payload)
print 'response:'
print(r.status_code)
print r.content
