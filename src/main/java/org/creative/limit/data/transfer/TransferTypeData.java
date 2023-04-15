package org.creative.limit.data.transfer;

import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataSerializable;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.item.inventory.ItemStack;

public final class TransferTypeData {


    private static Key<Value<TransferType>> TRANSFER_TYPE;

    private TransferTypeData() {
        throw new RuntimeException("Should not create");
    }

    @ApiStatus.Internal
    public static void setTransferTypeKey(Key<Value<TransferType>> key) {
        TRANSFER_TYPE = key;
    }

    public static Key<Value<TransferType>> getTransferTypeKey() {
        return TRANSFER_TYPE;
    }


    static final DataQuery ENUM_VALUE = DataQuery.of("transfer_type_value");
}
