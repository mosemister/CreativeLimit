package org.creative.limit.data.transfer;

import org.spongepowered.api.data.persistence.*;

public enum TransferType implements DataSerializable {

    FROM_CREATIVE,
    FROM_NONE_CREATIVE;

    @Override
    public int contentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        return DataContainer.createNew().set(Queries.CONTENT_VERSION, this.contentVersion()).set(TransferTypeData.ENUM_VALUE, this.name());
    }
}
