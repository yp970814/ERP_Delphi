# WechatApplet
#项目是传统项目，无法使用@RequestMapping注解来标注接口
#所以通过HttpServletRequest.getRequestURI()，解析请求url，通过Servlet反射来实现接口
#通过反射实现接口，类必须实例化，所以用static修饰

#WechatController，微信小程序的接口，其中有微信小程序api的一些实现，如：获取openid、手机号、消息订阅等
#由于是内部使用，session_key是以明文传输给前台的，这样做是不安全的，如用uuid，将uuid和session_key关联缓存，返回uuid给前端

#WechatApi，传统项目没有redis，就用到了guava做缓存，缓存微信官方返回的access_token，用作消息推送

#这里强调一些微信小程序的小知识
#模板消息，需要fromid，需要用户主动获取，且有效期只有7天，后被官方取消
#一次性消息订阅，相当于升级版的模板消息，有效期无限，但也是需要用户主动获取
#长期消息订阅，只针对公共服务开放模板，一般公司没有资格
#由于，一次性消息订阅不符合业务需求，而长期消息订阅没有资格
#就用统一服务消息，微信公共号绑定微信小程序，通过微信小程序发送消息
