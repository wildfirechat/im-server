部署IM服务后，可以使用Server SDK通过API来检查服务的部署情况，可以执行:
```shell
export WILDFIRECHAT_ADMIN_SECRET_KEY='<the same secret configured on the server>'
java -jar checker.jar http://127.0.0.1:18080 http://127.0.0.1 false false
```
来检查。检查之后会产生垃圾数据，请在检查之后停掉IM服务后，再删除掉IM服务数据库。此检查仅检查API，检查通过并不代表部署肯定成功，还需要客户端来验证。
