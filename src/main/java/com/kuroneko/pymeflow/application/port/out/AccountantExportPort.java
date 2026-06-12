package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.application.export.MonthlyCashflowSummary;
import com.kuroneko.pymeflow.domain.export.ExportPeriod;
import com.kuroneko.pymeflow.domain.tenant.TenantId;

public interface AccountantExportPort {
    ExportResult export(MonthlyCashflowSummary summary);

    ExportResult export(ExportPeriod period, TenantId tenantId);

    record ExportResult(String mediaType, byte[] content) {
        public ExportResult {
            if (mediaType == null || mediaType.isBlank()) {
                throw new IllegalArgumentException("Media type is required");
            }
            content = content == null ? new byte[0] : content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
