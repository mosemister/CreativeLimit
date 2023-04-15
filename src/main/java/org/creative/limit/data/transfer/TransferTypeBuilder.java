package org.creative.limit.data.transfer;

import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataBuilder;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;

import java.util.Optional;

public class TransferTypeBuilder extends AbstractDataBuilder<TransferType> {

    public TransferTypeBuilder() {
        super(TransferType.class, 1);
    }

    @Override
    protected Optional<TransferType> buildContent(DataView container) throws InvalidDataException {
        Optional<String> enumValue = container.getString(TransferTypeData.ENUM_VALUE);
        return enumValue.map(TransferType::valueOf);
    }
}
