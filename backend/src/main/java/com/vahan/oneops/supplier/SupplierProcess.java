package com.vahan.oneops.supplier;

import com.vahan.oneops.project.StageMaster;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One process a supplier does (for example Wire Cut) and who to call about it. */
@Entity
@Table(name = "supplier_process")
public class SupplierProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_master_id", nullable = false)
    private StageMaster stageMaster;

    @Column(name = "contact_person", length = 120)
    private String contactPerson;

    @Column(name = "contact_phone", length = 80)
    private String contactPhone;

    protected SupplierProcess() {
    }

    public SupplierProcess(Supplier supplier, StageMaster stageMaster, String contactPerson, String contactPhone) {
        this.supplier = supplier;
        this.stageMaster = stageMaster;
        this.contactPerson = contactPerson;
        this.contactPhone = contactPhone;
    }

    public Long getId() { return id; }
    public Supplier getSupplier() { return supplier; }
    public StageMaster getStageMaster() { return stageMaster; }
    public String getContactPerson() { return contactPerson; }
    public String getContactPhone() { return contactPhone; }
}
