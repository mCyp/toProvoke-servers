package net.qiujuer.web.italker.push.utils;

import com.gexin.rp.sdk.base.IBatch;
import com.gexin.rp.sdk.base.IIGtPush;
import com.gexin.rp.sdk.base.IPushResult;
import com.gexin.rp.sdk.base.impl.SingleMessage;
import com.gexin.rp.sdk.base.impl.Target;
import com.gexin.rp.sdk.http.IGtPush;
import com.gexin.rp.sdk.template.TransmissionTemplate;
import com.google.common.base.Strings;
import net.qiujuer.web.italker.push.bean.api.base.PushModel;
import net.qiujuer.web.italker.push.bean.db.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 消息推送工具类
 * Created by Administrator on 2017/8/29.
 */
public class PushDispatcher {
    //采用"Java SDK 快速入门"， "第二步 获取访问凭证 "中获得的应用配置，用户可以自行替换

    private static final String appId = "SsXA4yIx64AYlmuQDac1J7";
    private static final String appKey = "Aq6ovJRLmo5srLcjk5Wuz8";
    private static final String masterSecret = "kl6c9cfX0L8pTwUcV4e1z4";
    private static final String host = "http://sdk.open.api.igexin.com/apiex.htm";
    private final IIGtPush pusher;
    // 要收到消息的人的列表
    private final List<BatchBean> beans = new ArrayList<>();

    public PushDispatcher() {
        // 最根本的发送者
        pusher = new IGtPush(host, appKey, masterSecret);
    }

    /**
     * 添加一条消息
     * @param receiver 接受者
     * @param model 接受的model
     * @return 是否接受成功
     */
    public boolean add(User receiver, PushModel model){
        // 基础的检查
        if(receiver == null || Strings.isNullOrEmpty(receiver.getPushId()) || model == null)
            return false;

        String pushString = model.getPushString();
        if(Strings.isNullOrEmpty(pushString))
            return false;

        // 构建一个目标加内容
        BatchBean bean = buildMessage(receiver.getPushId(),pushString);
        beans.add(bean);
        return true;
    }

    /**
     * 对要发送的数据进行格式化封装
     * @param clientId 接受者的ID
     * @param text 发送的内容
     * @return BatchBean
     */
    private BatchBean buildMessage(String clientId,String text){
        // 透传消息，不是把消息显示到通知栏 而是把消息发送给MessageReceiver
        TransmissionTemplate template = new TransmissionTemplate();
        template.setAppId(appId);
        template.setAppkey(appKey);
        template.setTransmissionContent(text);
        template.setTransmissionType(0); // 这个Type为int型，填写1则自动启动app

        SingleMessage message = new SingleMessage();
        message.setData(template);  // 把透传消息设置到消息模板中
        message.setOffline(true); // 是否可以离线发送
        message.setOfflineExpireTime(24*3600*1000); // 离线消息的时常
        // 设置推送目标，填入appid和*clientId
        Target target = new Target();
        target.setAppId(appId);
        target.setClientId(clientId);
        return new BatchBean(message,target);
    }

    // 进行消息的最终发送
    public boolean submit(){
        // 构架打包的工具类
        IBatch batch = pusher.getBatch();

        // 是否有数据
        boolean havaData = false;
        for (BatchBean bean : beans) {
            try {
                batch.add(bean.message,bean.target);
                havaData = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!havaData)
            return false;

        // 如果有数据
        IPushResult result = null;
        try {
            result = batch.submit();
        } catch (IOException e) {
            e.printStackTrace();

            // 失败情况下尝试重复发送一次
            try {
                batch.retry();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        // 打印日志
        if(result != null){
            try {
                Logger.getLogger("PushDispatcher").log(Level.INFO,(String) result.getResponse().get("result"));
                return true;
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        Logger.getLogger("PushDispatcher").log(Level.WARNING,"推送服务器异常！！！");
        return false;
    }

    // 给每个人发送消息的Bean的封装
    private static class BatchBean{
        SingleMessage message;
        Target target;

        public BatchBean(SingleMessage message, Target target) {
            this.message = message;
            this.target = target;
        }
    }
}
