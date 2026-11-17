package com.sky.config;

import com.sky.common.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.entity.Orders;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MyMetaObjectHandlerTest {

    private final MyMetaObjectHandler handler = new MyMetaObjectHandler();

    @AfterEach
    void clearContext() {
        BaseContext.removeCurrentId();
    }

    @Test
    void shouldIgnoreAuditFieldsMissingFromAddressBook() {
        AddressBook addressBook = new AddressBook();
        MetaObject metaObject = SystemMetaObject.forObject(addressBook);
        BaseContext.setCurrentId(99L);

        assertDoesNotThrow(() -> handler.updateFill(metaObject));
        assertNotNull(addressBook.getUpdateTime());
    }

    @Test
    void shouldIgnoreEntitiesWithoutAuditFields() {
        Orders orders = new Orders();
        MetaObject metaObject = SystemMetaObject.forObject(orders);
        BaseContext.setCurrentId(99L);

        assertDoesNotThrow(() -> handler.updateFill(metaObject));
    }
}
