package net.qiujuer.web.italker.push.factory;

import net.qiujuer.web.italker.push.bean.api.group.GroupApplyModel;
import net.qiujuer.web.italker.push.bean.db.Apply;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.utils.Hib;

import java.time.LocalDateTime;

/**
 * 申请加好友或者加群辅助工具类
 * Created by Administrator on 2017/9/6.
 */
public class ApplyFactory {

    public static Apply createGroupApply(String userId, String groupId, GroupApplyModel model) {
        return Hib.query(session -> {
            Apply apply = new Apply();
            User user = UserFactory.findById(userId);

            apply.setType(Apply.TYPE_ADD_GROUP);
            apply.setApplicant(user);
            apply.setApplicantId(userId);
            apply.setAttach(model.getAttach());
            apply.setDecription(model.getDecription());
            apply.setTargetId(groupId);

            // 保存数据库
            session.save(apply);
            return  apply;
        });
    }
}
