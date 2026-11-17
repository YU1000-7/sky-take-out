package com.sky.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.sky.common.context.BaseContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        if (metaObject.hasSetter("createTime")) {
            strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        }
        if (metaObject.hasSetter("updateTime")) {
            strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        }

        Long currentId = BaseContext.getCurrentId();
        if (currentId != null) {
            if (metaObject.hasSetter("createUser")) {
                strictInsertFill(metaObject, "createUser", Long.class, currentId);
            }
            if (metaObject.hasSetter("updateUser")) {
                strictInsertFill(metaObject, "updateUser", Long.class, currentId);
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasSetter("updateTime")) {
            metaObject.setValue("updateTime", LocalDateTime.now());
        }

        Long currentId = BaseContext.getCurrentId();
        if (currentId != null && metaObject.hasSetter("updateUser")) {
            metaObject.setValue("updateUser", currentId);
        }
    }
}
