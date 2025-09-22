isc.defineClass('SSRCM_Lines_Grid', isc.OBGrid);

isc.defineClass('SSRCM_Combo_Grid', isc.OBGrid);


var SSRCM = {};

SSRCM.simplifyFamilies = function(families) {
    var newFamilies = {};
    var keys = Object.keys(families);

    for (var i = 0; i < keys.length; i++) {
        newFamilies[keys[i]] = {};
        newFamilies[keys[i]].newRequiredQty = families[keys[i]].newRequiredQty
        newFamilies[keys[i]].requiredQty = families[keys[i]].requiredQty

        newFamilies[keys[i]].products = []
        var products = families[keys[i]].products

        for (var j = 0; j < products.length; j++) {
            product = {};
            product.id = products[j].id
            if (products[j].description !== null && products[j].description !== undefined) {
                product.description = products[j].description
            } else {
                product.description = ''
            }
            if (products[j].insertedQty > 0) {
                product.insertedQty = products[j].insertedQty
                newFamilies[keys[i]].products.push(product)
            }
        }
    }
    return newFamilies;
}

SSRCM.fieldsGrid = [

    {
        name: 'lineNo',
        id: 'A7C7DE194E0E4069B778C187BDB8CDEE',
        title: OB.I18N.getLabel('SSRCM_LineNo'),
        columnName: 'Line',
        inpColumnName: 'inpline',
        sort: 1,
        selectOnClick: true,
        canSort: true,
        canFilter: true,
        showIf: 'false',
        showHover: true,
        filterOnKeypress: false,
        type: '_id_11'
    },
    {
        name: 'combo',
        id: '21DD88212EE34F0A85468CEC767AA8BC',
        title: OB.I18N.getLabel('SSRCM_Combo'),
        columnName: 'M_Offer_ID',
        inpColumnName: 'inpmOfferId',
        refColumnName: 'M_Offer_ID',
        targetEntity: 'PricingAdjustment',
        sort: 2,
        displayField: 'combo$_identifier',
        displaylength: 32,
        fkField: true,
        canFilter: false,
        selectOnClick: true,
        canSort: true,
        showHover: true,
        gridProps: {
            displayProperty: 'printName'
        },
        type: '_id_19'
    },
    {
        name: 'comboQty',
        id: '1A9ABE8890A34EA18DB231FD87DB37FA',
        title: OB.I18N.getLabel('SSRCM_ComboQuantity'),
        required: true,
        hasDefaultValue: true,
        columnName: 'Combo_Qty',
        inpColumnName: 'inpcomboQty',
        sort: 3,
        selectOnClick: true,
        canSort: true,
        canFilter: true,
        showHover: true,
        filterOnKeypress: false,
        type: '_id_29'
    },
    {
        name: 'product',
        id: '705D5C1CE61748A19EE84A19B8F2A391',
        title: OB.I18N.getLabel('SSRCM_Product'),
        columnName: 'M_Product_ID',
        inpColumnName: 'inpmProductId',
        refColumnName: 'M_Product_ID',
        targetEntity: 'Product',
        sort: 4,
        displayField: 'product$_identifier',
        displaylength: 32,
        fkField: true,
        selectOnClick: true,
        canSort: true,
        canFilter: false,
        showHover: true,
        type: '_id_19'
    },
    {
        name: 'productCategory',
        id: 'EE1105BE2ADB42B182D48476EBBDDCCD',
        title: OB.I18N.getLabel('SSRCM_ProductCategory'),
        columnName: 'M_Product_Category_ID',
        inpColumnName: 'inpmProductCategoryId',
        refColumnName: 'M_Product_Category_ID',
        targetEntity: 'ProductCategory',
        sort: 5,
        showIf: 'false',
        editorProps: {
            displayField: '_identifier',
            valueField: 'id'
        },
        displaylength: 32,
        fkField: true,
        selectOnClick: true,
        canSort: true,
        canFilter: true,
        showHover: true,
        displayProperty: 'name',
        type: '_id_2A9888BFC01948B29EEC1887A1824299'
    },
    /*{
      name: 'stock',
      id: 'B82E4AE24FFC4347B577F7901739815E',
      title: OB.I18N.getLabel('SSRCM_Stock'),
      required: true,
      disabled: true,
      columnName: 'Stock',
      inpColumnName: 'inpstock',
      sort: 6,
      selectOnClick: true,
      canSort: true,
      canFilter: true,
      showHover: true,
      filterOnKeypress: false,
      type: '_id_29'
    },*/
    {
        name: 'orderedQuantity',
        id: 'EB2B8257BFE44024B7E62D9260DC6299',
        title: OB.I18N.getLabel('SSRCM_OrderedQuantity'),
        required: true,
        columnName: 'Qtyordered',
        inpColumnName: 'inpqtyordered',
        sort: 7,
        selectOnClick: true,
        canSort: true,
        canFilter: true,
        showHover: true,
        filterOnKeypress: false,
        type: '_id_29'
    },
    {
        name: 'grossUnitPrice',
        id: '7207133E7B83403AA2CA532685DB6FCA',
        title: OB.I18N.getLabel('SSRCM_GrossUnitPrice'),
        required: true,
        hasDefaultValue: true,
        columnName: 'Gross_Unit_Price',
        inpColumnName: 'inpgrossUnitPrice',
        gridProps: {
            sort: 14,
            showIf: 'false',
            selectOnClick: true,
            canSort: true,
            canFilter: true,
            showHover: true,
            filterOnKeypress: false
        },
        type: '_id_800008'
    },
    /*  {
        name: 'netUnitPrice',
        id: 'EF0AA98DCD654BCBA5784DFFA82EEDAA',
        title: OB.I18N.getLabel('SSRCM_NetUnitPrice'),
        required: true,
        disabled: true,
        columnName: 'Price',
        inpColumnName: 'inpprice',
        sort: 8,
        selectOnClick: true,
        canSort: true,
        canFilter: true,
        showHover: true,
        filterOnKeypress: false,
        type: '_id_800008'
      },

      {
        name: 'tax',
        id: 'B0DA8A141F204F4B8B18A38D44BCAFEA',
        title: OB.I18N.getLabel('SSRCM_Tax'),
        disabled: true,
        columnName: 'C_Tax_ID',
        inpColumnName: 'inpcTaxId',
        refColumnName: 'C_Tax_ID',
        targetEntity: 'FinancialMgmtTaxRate',
        sort: 9,
        displayField: 'tax$_identifier',
        displaylength: 32,
        fkField: true,
        selectOnClick: true,
        canSort: true,
        canFilter: false,
        showHover: true,
        criteriaField: 'tax$name',
        criteriaDisplayField: 'name',
        displayProperty: 'name',
        type: '_id_158'
      },*/
    {
        name: 'subtotal',
        id: '7F5F5EA2B30245A1BBE56757E0B6808B',
        title: OB.I18N.getLabel('SSRCM_Subtotal'),
        required: true,
        disabled: true,
        columnName: 'Subtotal',
        inpColumnName: 'inpsubtotal',
        sort: 11,
        selectOnClick: true,
        canSort: true,
        // showIf: 'false',
        canFilter: true,
        showHover: true,
        filterOnKeypress: false,
        type: '_id_12'
    },
    {
        name: 'lineNetAmount',
        id: '33293627DC1D4BD1B127B7A55835F48C',
        title: OB.I18N.getLabel('SSRCM_LineNetAmount'),
        required: true,
        disabled: true,
        columnName: 'Linenetamt',
        inpColumnName: 'inplinenetamt',
        sort: 10,
        selectOnClick: true,
        canSort: true,
        canFilter: true,
        showHover: true,
        filterOnKeypress: false,
        type: '_id_12'
    },
    {
        name: 'description',
        id: '4275F748A8AE4AADA209709CE4731E47',
        title: OB.I18N.getLabel('SSRCM_Description'),
        colSpan: 3,
        rowSpan: 1,
        columnName: 'Description',
        inpColumnName: 'inpdescription',
        "length": 2000,
        sort: 12,
        length: 2000,
        displaylength: 2000,
        editorType: 'OBPopUpTextAreaItem',
        selectOnClick: true,
        canFilter: true,
        showHover: true,
        canSort: false,
        type: '_id_14'
    },
    {
        name: 'creationDate',
        title: OB.I18N.getLabel('SSRCM_CreationDate'),
        disabled: true,
        updatable: false,
        personalizable: false,
        sort: 990,
        cellAlign: 'left',
        showIf: 'false',
        canSort: true,
        canFilter: true,
        showHover: true,
        type: '_id_16'
    },
    {
        name: 'createdBy',
        title: OB.I18N.getLabel('SSRCM_CreatedBy'),
        disabled: true,
        updatable: false,
        personalizable: false,
        targetEntity: 'User',
        displayField: 'createdBy$_identifier',
        sort: 990,
        cellAlign: 'left',
        showIf: 'false',
        fkField: true,
        canSort: true,
        canFilter: true,
        showHover: true,
        type: '_id_30'
    },
    {
        name: 'updated',
        title: OB.I18N.getLabel('SSRCM_Updated'),
        disabled: true,
        updatable: false,
        personalizable: false,
        sort: 990,
        cellAlign: 'left',
        showIf: 'false',
        canSort: true,
        canFilter: true,
        showHover: true,
        type: '_id_16'
    },
    {
        name: 'updatedBy',
        title: OB.I18N.getLabel('SSRCM_UpdatedBy'),
        disabled: true,
        updatable: false,
        personalizable: false,
        targetEntity: 'User',
        displayField: 'updatedBy$_identifier',
        sort: 990,
        cellAlign: 'left',
        showIf: 'false',
        fkField: true,
        canSort: true,
        canFilter: true,
        showHover: true,
        type: '_id_30'
    },
    {
        name: 'isdefault',
        id: 'F47DCD52E0D14C07A1CB4D02D6107890',
        title: 'Isdefault',
        hasDefaultValue: true,
        columnName: 'Isdefault',
        inpColumnName: 'inpisdefault',
        "width": 1,
        "overflow": "visible",
        showIf: 'false',
        type: '_id_20'
    },
    {
        name: 'istaxinclude',
        id: '110075188C8C429D8B975324D8D823F2',
        title: 'Istaxinclude',
        hasDefaultValue: true,
        columnName: 'Istaxinclude',
        inpColumnName: 'inpistaxinclude',
        "width": 1,
        "overflow": "visible",
        showIf: 'false',
        yesNo: true,
        type: '_id_20'
    },
]

SSRCM.fieldsFormProduct = [

    {
        name: 'product',
        id: '705D5C1CE61748A19EE84A19B8F2A391',
        title: OB.I18N.getLabel('SSRCM_Product'),
        columnName: 'M_Product_ID',
        inpColumnName: 'inpmProductId',
        refColumnName: 'M_Product_ID',
        targetEntity: 'Product',
        type: '_id_CB6C648A6CAB4F749CB25EB1EBC4BAB5'
    },
    {
        name: 'orderedQuantity',
        id: 'EB2B8257BFE44024B7E62D9260DC6299',
        title: OB.I18N.getLabel('SSRCM_OrderedQuantity'),
        required: true,
        hasDefaultValue: true,
        columnName: 'Qtyordered',
        inpColumnName: 'inpqtyordered',
        type: '_id_29'
    },
    {
        name: 'grossUnitPrice',
        id: '7207133E7B83403AA2CA532685DB6FCA',
        title: OB.I18N.getLabel('SSRCM_GrossUnitPrice'),
        required: true,
        disabled: true,
        hasDefaultValue: true,
        columnName: 'Gross_Unit_Price',
        inpColumnName: 'inpgrossUnitPrice',
        type: '_id_800008'
    },
    {
        name: 'lineNetAmount',
        id: '33293627DC1D4BD1B127B7A55835F48C',
        title: OB.I18N.getLabel('SSRCM_LineNetAmount'),
        required: true,
        disabled: true,
        // colSpan: 5,
        hasDefaultValue: true,
        columnName: 'Linenetamt',
        inpColumnName: 'inplinenetamt',
        type: '_id_12'
    },
    {
        name: 'productCategory',
        id: 'EE1105BE2ADB42B182D48476EBBDDCCD',
        title: OB.I18N.getLabel('SSRCM_ProductCategory'),
        disabled: true,
        columnName: 'M_Product_Category_ID',
        inpColumnName: 'inpmProductCategoryId',
        refColumnName: 'M_Product_Category_ID',
        targetEntity: 'ProductCategory',
        type: '_id_14',
        colSpan: 1,
        rowSpan: 1,
    },
    {
        name: 'stock',
        id: 'B82E4AE24FFC4347B577F7901739815E',
        title: OB.I18N.getLabel('SSRCM_Stock'),
        required: true,
        disabled: true,
        hasDefaultValue: true,
        columnName: 'Stock',
        inpColumnName: 'inpstock',
        type: '_id_29'
    },
    {
        name: 'netUnitPrice',
        id: 'EF0AA98DCD654BCBA5784DFFA82EEDAA',
        title: OB.I18N.getLabel('SSRCM_NetUnitPrice'),
        required: true,
        disabled: true,
        hasDefaultValue: true,
        columnName: 'Price',
        inpColumnName: 'inpprice',
        type: '_id_800008'
    },
    {
        name: 'subtotal',
        id: '7F5F5EA2B30245A1BBE56757E0B6808B',
        title: OB.I18N.getLabel('SSRCM_Subtotal'),
        required: true,
        disabled: true,
        hasDefaultValue: true,
        columnName: 'Subtotal',
        inpColumnName: 'inpsubtotal',
        type: '_id_12'
    },
    {
        name: 'description',
        id: '4275F748A8AE4AADA209709CE4731E47',
        title: OB.I18N.getLabel('SSRCM_Description'),
        colSpan: 2,
        rowSpan: 2,
        columnName: 'Description',
        inpColumnName: 'inpdescription',
        "length": 2000,
        type: '_id_14'
    },
    {
        name: '',
        personalizable: false,
        type: 'spacer'
    },
    {
        name: 'tax',
        id: 'B0DA8A141F204F4B8B18A38D44BCAFEA',
        title: OB.I18N.getLabel('SSRCM_Tax'),
        disabled: true,
        columnName: 'C_Tax_ID',
        inpColumnName: 'inpcTaxId',
        refColumnName: 'C_Tax_ID',
        targetEntity: 'FinancialMgmtTaxRate',
        type: '_id_158'
    },
    {
        name: 'orderLineId',
        title: OB.I18N.getLabel('SSRCM_OrderLineId'),
        type: '_id_14',
        hidden: true,
        showIf(item, value, form, values) {
            return false;
        }
    },
    {
        name: 'taxRate',
        title: OB.I18N.getLabel('SSRCM_TaxRate'),
        hidden: true,
        showIf: 'false',
        type: '_id_12'
    },
    {
        name: 'isdefault',
        id: 'F47DCD52E0D14C07A1CB4D02D6107890',
        title: 'Isdefault',
        columnName: 'Isdefault',
        inpColumnName: 'inpisdefault',
        hidden: true,
        showIf: 'false',
        type: '_id_20'
    },
    {
        name: 'istaxinclude',
        id: '110075188C8C429D8B975324D8D823F2',
        title: 'Istaxinclude',
        hidden: true,
        hasDefaultValue: true,
        columnName: 'Istaxinclude',
        inpColumnName: 'inpistaxinclude',
        yesNo: true,
        showIf: 'false',
        type: '_id_20'
    },
];

isc.SSRCM_Lines_Grid.addProperties({
    dataSource: null,
    showFilterEditor: true,
    heigth: '80%',
    dataProperties: {
        useClientFiltering: false
    },

    gridFields: SSRCM.fieldsGrid,

    resetEmptyMessage() {

    },
    setDataSource: function(ds) {
        this.Super('setDataSource', [ds, this.gridFields]);
        this.refreshFields();
        this.sort('lineNo');
    },

    initWidget: function() {
        this.Super('initWidget', arguments);
        this.parent = arguments[0].parent;
        OB.Datasource.get('saqb_orderline', this, null, true);
        var me = this;
        this.selectionChanged = function(record, state) {
            this.Super('selectionChanged', {
                record: record,
                state: state
            });
            me.parent.onLineSelect(record, state);
        };
    },
    convertCriteria: function(criteria) {
        criteria = isc.addProperties({}, criteria || {});

        if (!criteria.criteria) {
            criteria.criteria = [];
        }

        if (criteria.criteria.push) {
            criteria.criteria.push({
                fieldName: "saqbOrder",
                operator: "equals",
                value: this.parent.getParentId()
            });
        }

        return criteria;

    },

    filterData: function(criteria, callback, requestProperties) {
        return this.Super('filterData', [
            this.convertCriteria(criteria), callback, requestProperties
        ]);
    },

});

isc.SSRCM_Combo_Grid.addProperties({

    heigth: '100%',
    dataProperties: {
        useClientFiltering: true
    },
    gridFields: [

        {
            name: 'product',
            id: '705D5C1CE61748A19EE84A19B8F2A391',
            title: OB.I18N.getLabel('SSRCM_Product'),
            columnName: 'M_Product_ID',
            inpColumnName: 'inpmProductId',
            refColumnName: 'M_Product_ID',
            targetEntity: 'Product',
            sort: 2,
            displayField: 'product$_identifier',
            displaylength: 5,
            fkField: true,
            selectOnClick: true,
            canSort: true,
            canFilter: false,
            showHover: true,
            type: '_id_19'
        },
        {
            name: 'orderedQuantity',
            id: 'EB2B8257BFE44024B7E62D9260DC6299',
            title: OB.I18N.getLabel('SSRCM_OrderedQuantity'),
            required: true,
            columnName: 'Qtyordered',
            inpColumnName: 'inpqtyordered',
            sort: 5,
            canEdit: true,
            selectOnClick: true,
            canSort: true,
            canFilter: true,
            showHover: true,
            filterOnKeypress: false,
            type: '_id_29'
        },
        {
            name: 'grossUnitPrice',
            id: '7207133E7B83403AA2CA532685DB6FCA',
            title: OB.I18N.getLabel('SSRCM_GrossUnitPrice'),
            required: true,
            disabled: true,
            hasDefaultValue: true,
            columnName: 'Gross_Unit_Price',
            inpColumnName: 'inpgrossUnitPrice',
            sort: 6,
            selectOnClick: true,
            canSort: true,
            canFilter: true,
            showHover: true,
            filterOnKeypress: false,
            type: '_id_800008'
        },
        {
            name: 'description',
            id: '4275F748A8AE4AADA209709CE4731E47',
            title: OB.I18N.getLabel('SSRCM_Description'),
            columnName: 'Description',
            inpColumnName: 'inpdescription',
            "length": 2000,
            sort: 12,
            canEdit: true,
            length: 2000,
            displaylength: 2000,
            editorType: 'OBPopUpTextAreaItem',
            selectOnClick: true,
            canFilter: true,
            showHover: true,
            canSort: false,
            type: '_id_14'
        },
    ],

    resetEmptyMessage() {

    },

    initWidget: function() {
        this.Super('initWidget', arguments);
        this.parent = arguments[0].parent;
        this.data = arguments[0].datasource;
        this.sort('orderedQuantity');
        this.fields = this.gridFields;
        this.autoSaveEdits = true;

        var me = this;
        this.cellChanged = function(record, newValue, oldValue, rowNum, colNum, grid) {
            this.Super('cellChanged', {
                record: record,
                newValue: newValue,
                oldValue: oldValue,
                rowNum: rowNum,
                colNum: colNum,
                grid: grid
            });
            me.parent.cellChanged(record, newValue, oldValue, rowNum, colNum, grid);
        };
    }
});

isc.defineClass("SSRCM_Lines", isc.OBBaseView);
isc.SSRCM_Lines.addProperties({
    // do some margins between the members
    membersMargin: 10,
    defaultLayoutAlign: 'top',
    toolBar: null,
    viewGrid: null,
    hLayout: null,
    productComboForm: null,
    families: null,
    hLayoutInternal: null,
    comboQty: null,
    parentId: null,
    selecteLineId: null,
    comboLines: null,
    comboList: null,
    productList: null,
    tabs: [],
    hasValidState: function() {
        return true;
    },
    roleCanCreateRecords() {
        return false;
    },
    buildStructure: function() {
        // console.log("building structure");
    },
    updateViewBasedOnUiPattern: function() {
        // console.log("Updating");
    },
    updateSubtabVisibility: function() {
        // console.log("updateSubtabVisibility");
        return null;
    },
    setAsActiveView() {
        this.standardWindow.autoSave()
    },
    setAsActiveViewfunction() {
        return;
    },
    setReadOnly: function(params) {
        // console.log(params);
    },
    setSingleRecord: function() {},
    setEditOrDeleteOnly: function() {},
    doRefreshContents: function() {
        this.viewGrid.fetchData({
            criteria: {
                "fieldName": "saqbOrder",
                "operator": "equals",
                "value": this.getParentId()
            }
        });

        var childrenList = this.parentElement.parentElement.parentContainer.toolBar.children;
        for (var i = 0; i < childrenList.length; i++) {
            if (childrenList[i].buttonType === "eliminate") {
                var children = childrenList[i];
                children.action = function() {
                    children.parentElement.view.deleteSelectedRows()
                }
                break;
            }
        }

        this.parentId = this.getParentId();
        if (this.getParentRecord()) {
            var documentStatus = this.getParentRecord().documentStatus;
        }

        if (this.parentId && documentStatus !== "CO") {
            this.addProduct.disabled = false;
            this.addProduct.setState("");
            this.addCombo.disabled = false;
            this.addCombo.setState("");
            this.insertDefaultProduct();
        } else {
            this.addProduct.disabled = true;
            this.addProduct.setState("Disabled");
            this.addCombo.disabled = true;
            this.addCombo.setState("Disabled");
            children.action = function() {
                isc.say(OB.I18N.getLabel('SSRCM_Disabled_For_Completed_Document'));
            }
        }

        this.vLayoutForms.removeMember(this.vLayoutForms.getMembers());
        this.deleteLine.setState("Disabled");
        this.deleteLine.disabled = true;


    },
    onLineSelect: function(record, state) {
        var me = this;
        if (me.getParentRecord().documentStatus === "CO") {
            return;
        }


        if (record.isdefault) {
            me.deleteLine.setState("Disabled");
            me.deleteLine.disabled = true;
            me.selecteLineId = null;
            me.vLayoutForms.removeMember(me.vLayoutForms.getMembers());
            return;
        } else {
            me.deleteLine.setState("");
            me.deleteLine.disabled = false;
            me.selecteLineId = record.id;
            me.vLayoutForms.removeMember(me.vLayoutForms.getMembers());
        }
        var childrenList = this.parentElement.parentElement.parentContainer.toolBar.children;
        for (var i = 0; i < childrenList.length; i++) {
            if (childrenList[i].buttonType === "eliminate") {
                childrenList[i].action = function() {
                    if (me.selecteLineId !== null && !record.isdefault) {
                        me.deleteLineAction(me.selecteLineId, me.parentId);
                        //childrenList[i].parentElement.view.deleteSelectedRows()
                    } else {

                        //me.deleteLineAction(me.selecteLineId, me.parentId);
                        childrenList[i].parentElement.view.deleteSelectedRows()
                        isc.say(OB.I18N.getLabel('SSRCM_ELIMINATE_DEFAULT_PRD'));
                    }
                }
                break;
            }
        }


        if (record.combo) {
            var lines = me.viewGrid.data.localData.filter(function(line) {
                return (line.combo === record.combo);
            });

            me.comboLines = lines;
            me.hLayoutInternal = isc.HLayout.create({
                // showEdges: true,
                width: '100%',
                height: '100%'
            });
            var vLayoutInternal = isc.VLayout.create({
                width: '140px',
                height: '100%',
                defaultLayoutAlign: 'center',
                align: 'center',
                membersMargin: 10
            });

            me.productComboForm = isc.OBViewForm.create({
                membersMargin: 10,
                itemChange: function(item, newValue) {
                    if (item.valueMap && item.valueMap[newValue]) {
                        me.loadFamilies(newValue);
                    }
                    if (item.editorType === "spinner") {
                        me.comboQtyChange(newValue);
                    }

                },
                canEdit: true,
                canEditField: function() {
                    return true;
                },
                fields: [{
                    titleAlign: 'top',
                    name: 'combo',
                    title: OB.I18N.getLabel('SSRCM_Combo'),
                    required: true,
                    canFilter: true,
                    type: '_id_CB6C648A6CAB4F749CB25EB1EBC4BAB5',
                    valueMap: me.comboList,
                    value: record.combo
                }, {
                    name: '',
                    type: 'spacer'
                }, {
                    title: OB.I18N.getLabel('SSRCM_quantiy'),
                    name: "quantity",
                    showTitle: false,
                    editorType: "spinner",
                    writeStackedIcons: false,
                    defaultValue: record.comboQty,
                    min: 1,
                    step: 1,
                    max: 9999999999
                }]
            });

            me.loadFamilies(record.combo, lines);

            vLayoutInternal.addMember(me.productComboForm);
            vLayoutInternal.addMember(me.insertCombo);
            me.hLayoutInternal.addMember(vLayoutInternal);
            me.vLayoutForms.addMember(me.hLayoutInternal);


            me.insertCombo.setState("Disabled");
            me.insertCombo.disabled = true;
            me.insertCombo.title = OB.I18N.getLabel('SSRCM_modify_combo_lines');
        } else {
            me.productComboForm = isc.OBViewForm.create({
                width: '80%',
                heigth: '100%',
                numCols: 8,
                itemChange: function(item, newValue) {
                    if (item.valueMap && item.valueMap[newValue]) {
                        me.getProductPriceAndStock(me.parentId, newValue)
                    } else if (item.columnName === "Qtyordered") {

                        me.changeQtyOrdered(newValue);
                    } /*else if (item.columnName === "Description") {
                        me.insertProduct.disabled = false;
                        me.insertProduct.setState("");
                    }*/

                },
                fields: SSRCM.fieldsFormProduct
            });

            me.getProductPriceAndStock(me.parentId, record.product);
            me.productComboForm.setValue("orderLineId", record.id);
            me.productComboForm.setValue("product", record.product);
            me.productComboForm.setValue("product$_identifier", record.product$_identifier);
            me.productComboForm.setValue("tax", record.tax);
            me.productComboForm.setValue("tax$_identifier", record.tax$_identifier);
            me.productComboForm.setValue("subtotal", record.subtotal);

            var key = record.product;
            var obj = {};
            obj[key] = record.product$_identifier;
            me.productComboForm.setValueMap("product", obj);

            me.productComboForm.setValue("orderedQuantity", record.orderedQuantity);
            me.productComboForm.setValue("description", record.description);


            me.vLayoutForms.addMember(me.productComboForm);
            me.insertProduct.title = OB.I18N.getLabel('SSRCM_modify_line');
            me.vLayoutForms.addMember(me.insertProduct);
            me.insertProduct.setState("Disabled");
            me.insertProduct.disabled = true;
        }
    },
    getParentId: function() {
        var parentRecord = this.getParentRecord();
        if (parentRecord) {
            return parentRecord.id;
        }
    },
    getParentRecord: function() {
        var grid = null;
        // if there is no parent view, there is no parent record
        if (!this.parentView) {
            return null;
        }
        // use the standard tree of the tree grid depending on the view being shown
        if (this.parentView.isShowingTree) {
            grid = this.parentView.treeGrid;
        } else {
            grid = this.parentView.viewGrid;
        }
        // if the parent grid does not have exactly one selected record, return null
        if (!grid.getSelectedRecords() || grid.getSelectedRecords().length !== 1) {
            return null;
        }
        // a new parent is not a real parent
        if (!this.parentView.isShowingTree && this.parentView.viewGrid.getSelectedRecord()._new) {
            return null;
        }
        return grid.getSelectedRecord();
    },

    getProductPriceAndStock: function(saqbOrderID, productId) {

        if (productId === 'undefined') {
            return;
        }
        var me = this;
        if (!productId) {
            me.insertProduct.disabled = true;
            me.insertProduct.setState("Disabled");
            return;
        }

        var setValues = function(data) {
            me.productComboForm.setValue("stock", data.data.stock);
            me.productComboForm.setValue("productCategory", data.data.productCategory);
            me.productComboForm.setValue("netUnitPrice", data.data.netUnitPrice);
            me.productComboForm.setValue("grossUnitPrice", data.data.grossUnitPrice);
            me.productComboForm.setValue("tax", data.data.tax);
            me.productComboForm.setValue("tax$_identifier", data.data.taxIdentifier);
            me.productComboForm.setValue("taxRate", data.data.taxRate);
            me.productComboForm.setValue("istaxinclude", data.data.istaxinclude);

            if (me.productComboForm.getValue("orderedQuantity")) {

                me.changeQtyOrdered(me.productComboForm.getValue("orderedQuantity"));
                /* me.productComboForm.setValue("lineNetAmount", data.data.grossUnitPrice * me.productComboForm.getValue("orderedQuantity"));
                me.productComboForm.setValue("subtotal", data.data.netUnitPrice * me.productComboForm.getValue("orderedQuantity") * (1 + (data.data.taxRate / 100)));*/
            } else {
                me.productComboForm.setValue("lineNetAmount", 0);
                me.productComboForm.setValue("subtotal", 0);
            }

            if (me.productComboForm.getValue("orderedQuantity")
                /*&& me.productComboForm.getValue("stock") && (me.productComboForm.getValue("orderedQuantity") <= me.productComboForm.getValue("stock"))*/
            ) {
                me.insertProduct.disabled = false;
                me.insertProduct.setState("");
            } else {
                me.insertProduct.disabled = true;
                me.insertProduct.setState("Disabled");
            }
        }

        OB.RemoteCallManager.call('ec.com.sidesoft.retail.combo.webservices.ComboHelper', {}, {
            productId: productId,
            saqbOrderID: saqbOrderID,
            action: "getProductPriceAndStock"
        }, setValues);
    },

    changeQtyOrdered: function(newQty) {
        var me = this;

        if (newQty > 999999999) {
            isc.say(OB.I18N.getLabel('SSRCM_MAX_QUANTITY'));
        }

        if (!newQty) {
            me.insertProduct.disabled = true;
            me.insertProduct.setState("Disabled");
            return;
        }

        if (me.productComboForm.getValue("product") && newQty > 0) {
            me.getPrices(newQty);
            /*if (me.productComboForm.getValue('grossUnitPrice') != undefined) {
            //  me.productComboForm.setValue("lineNetAmount", me.productComboForm.getValue('grossUnitPrice') * newQty);
              me.productComboForm.setValue("subtotal", me.productComboForm.getValue('grossUnitPrice') * newQty * (1 + (me.productComboForm.getValue('taxRate') / 100)));

            } else {
              me.productComboForm.setValue("lineNetAmount", 0);
              me.productComboForm.setValue("subtotal", 0);
            }
             */
            me.insertProduct.disabled = false;
            me.insertProduct.setState("");
        } else {
            me.insertProduct.disabled = true;
            me.insertProduct.setState("Disabled");
        }


    },
    getPrices: function(newQty) {
        var me = this;

        var setValues = function(data) {

            //me.productComboForm.setValue("orderedQuantity", data.data.newqty);
            me.productComboForm.setValue("netUnitPrice", data.data.pricewithouttax);
            me.productComboForm.setValue("grossUnitPrice", data.data.pricewithtax);
            me.productComboForm.setValue("lineNetAmount", data.data.total);
            me.productComboForm.setValue("subtotal", data.data.subtotal);
        };
        me.parent
        OB.RemoteCallManager.call('ec.com.sidesoft.retail.combo.webservices.ComboHelper', {}, {
            productId: me.productComboForm.getValue('product'),
            saqbOrderID: me.parentId,
            orderedQuantity: newQty,
            action: "getPrices"
        }, setValues);
    },
    insertDefaultProduct: function() {
        var me = this;

        var refreshGrid = function() {
            me.viewGrid.fetchData({
                criteria: {
                    "fieldName": "saqbOrder",
                    "operator": "equals",
                    "value": me.getParentId()
                }
            });
            me.parentElement.parentElement.parentContainer.refreshCurrentRecord()
        };
        me.parent
        OB.RemoteCallManager.call('ec.com.sidesoft.retail.combo.webservices.ComboHelper', {}, {
            saqbOrderID: me.parentId,
            action: "insertDefaultProduct"
        }, refreshGrid);
    },
    insertProductLine: function() {
        var me = this;

        var refreshGrid = function() {
            me.viewGrid.fetchData({
                criteria: {
                    "fieldName": "saqbOrder",
                    "operator": "equals",
                    "value": me.getParentId()
                }
            });

            me.parentElement.parentElement.parentContainer.refreshCurrentRecord()
        };



        OB.RemoteCallManager.call('ec.com.sidesoft.retail.combo.webservices.ComboHelper', {}, {
            productId: me.productComboForm.getValue('product'),
            saqbOrderID: me.parentId,
            orderedQuantity: me.productComboForm.getValue('orderedQuantity'),
            description: me.productComboForm.getValue('description'),
            orderLineId: me.productComboForm.getValue('orderLineId'),
            grossUnitPrice: me.productComboForm.getValue('grossUnitPrice'),
            netUnitPrice: me.productComboForm.getValue('netUnitPrice'),
            subtotal: me.productComboForm.getValue('subtotal'),
            lineNetAmount: me.productComboForm.getValue('lineNetAmount'),
            istaxinclude: me.productComboForm.getValue('istaxinclude'),
            action: "insertProductLine"
        }, refreshGrid);
    },

    insertComboLine: function() {
        var me = this;

        var refreshGrid = function() {
            me.viewGrid.fetchData({
                criteria: {
                    "fieldName": "saqbOrder",
                    "operator": "equals",
                    "value": me.getParentId()
                }
            });
            me.parentElement.parentElement.parentContainer.refreshCurrentRecord()
        };

        OB.RemoteCallManager.call('ec.com.sidesoft.retail.combo.webservices.ComboHelper', {}, {
            comboConf: SSRCM.simplifyFamilies(me.families),
            saqbOrderID: me.parentId,
            comboLines: me.selecteLineId,
            action: "insertComboLine"
        }, refreshGrid);
    },

    deleteLineAction: function() {
        var me = this;

        var refreshGrid = function() {
            me.viewGrid.fetchData({
                criteria: {
                    "fieldName": "saqbOrder",
                    "operator": "equals",
                    "value": me.getParentId()
                }

            });
            me.deleteLine.setState("Disabled");
            me.deleteLine.disabled = true;
            me.selecteLineId = null;
            me.parentElement.parentElement.parentContainer.refreshCurrentRecord()
        };

        OB.RemoteCallManager.call('ec.com.sidesoft.retail.combo.webservices.ComboHelper', {}, {
            selecteLineId: me.selecteLineId,
            saqbOrderID: me.parentId,
            action: "deleteLine"
        }, refreshGrid);
    },
    setProducts: function() {
        var me = this;

        var setProductsValue = function(data) {
            me.productList = data.data;
            me.productComboForm.setValueMap('product', data.data);
        }

        OB.RemoteCallManager.call('ec.com.sidesoft.retail.combo.webservices.ComboHelper', {}, {
            saqbOrderID: me.parentId,
            action: "getProductList"
        }, setProductsValue);
    },
    setCombos: function() {
        var me = this;

        var setCombosValue = function(data) {
            me.comboList = data.data;
            me.productComboForm.setValueMap('combo', data.data);
        }

        OB.RemoteCallManager.call('ec.com.sidesoft.retail.combo.webservices.ComboHelper', {}, {
            saqbOrderID: me.parentId,
            action: "getComboList"
        }, setCombosValue);
    },


    loadFamilies: function(productId, lines) {
        var me = this;
        me.tabs = [];
        if (productId === 'undefined') {
            return;
        }

        if (!productId) {
            me.insertCombo.disabled = true;
            me.insertCombo.setState("Disabled");
            return;
        }

        if (!lines) {
            me.productComboForm.setValue('quantity', 1);
        }

        var setValues = function(data) {

            me.tabs = [];
            me.families = data.data;

            var familyIds = Object.keys(me.families);

            if (lines) {
                //actualizar datos
                for (var j = 0; j < lines.length; j++) {
                    var line = lines[j];
                    for (var i = 0; i < familyIds.length; i++) {
                        var family = me.families[familyIds[i]];
                        var products = family.products;

                        for (var k = 0; k < products.length; k++) {
                            var product = products[k];
                            if (product.id === line.product) {
                                product.insertedQty = line.orderedQuantity;
                                product.orderedQuantity = line.orderedQuantity;
                                product.grossUnitPrice = line.grossUnitPrice;
                                product.description = line.description;
                                family.newRequiredQty = line.comboQty * family.requiredQty;
                                family.alreadyFilledQty += line.orderedQuantity;
                            }
                        }
                    }
                }
            }

            for (var i = 0; i < familyIds.length; i++) {
                var family = me.families[familyIds[i]];

                var grid = isc.SSRCM_Combo_Grid.create({
                    parent: me,
                    datasource: family.products
                });

                var tab = {
                    title: family.name + ' ' + family.alreadyFilledQty + '/' + family.newRequiredQty,
                    iconSize: 16,
                    pane: grid
                };
                me.tabs.push(tab);
            }
            me.hLayoutInternal.addMember(
                isc.TabSet.create({
                    ID: "topTabSet",
                    tabBarPosition: "top",
                    width: '100%',
                    height: '100%',
                    tabs: me.tabs
                }));
        }

        OB.RemoteCallManager.call('ec.com.sidesoft.retail.combo.webservices.ComboHelper', {}, {
            productId: productId,
            saqbOrderID: me.parentId,
            action: "getDiscountDefinition"
        }, setValues);
    },

    cellChanged: function(record, newValue, oldValue, rowNum, colNum) {
        var me = this;

        var fullComboComplete = true;
        var familyIds = Object.keys(me.families);
        if (colNum == 3) {
            for (var i = 0; i < familyIds.length; i++) {
                var family = me.families[familyIds[i]];
                var productsOfTheFamily = family.products;

                for (var j = 0; j < productsOfTheFamily.length; j++) {
                    if (productsOfTheFamily[j].id === record.id) {
                        productsOfTheFamily[j].description = newValue;
                    }
                }
            }
            for (var i = 0; i < familyIds.length; i++) {
                var family = me.families[familyIds[i]];
                var productsOfTheFamily = family.products;
                fullComboComplete = fullComboComplete && (family.alreadyFilledQty === family.newRequiredQty);
            }
            if (fullComboComplete) {
                me.insertCombo.setState("");
                me.insertCombo.disabled = false;
            }
            return;
        }

        // if (newValue > record.stock) {
        //   record.orderedQuantity = oldValue;
        //   return;
        // }

        for (var i = 0; i < familyIds.length; i++) {
            var family = me.families[familyIds[i]];
            var productsOfTheFamily = family.products;

            for (var j = 0; j < productsOfTheFamily.length; j++) {
                if (productsOfTheFamily[j].id === record.id) {
                    productsOfTheFamily[j].insertedQty = Math.floor(newValue);
                    family.alreadyFilledQty = family.alreadyFilledQty + Math.floor(newValue) - oldValue;
                    me.hLayoutInternal.getMember(1).setTabTitle(me.tabs[i], family.name + ' ' + family.alreadyFilledQty + '/' + family.newRequiredQty);
                }
            }

            fullComboComplete = fullComboComplete && (family.alreadyFilledQty === family.newRequiredQty);
        }

        if (fullComboComplete) {
            me.insertCombo.setState("");
            me.insertCombo.disabled = false;
        } else {
            me.insertCombo.setState("Disabled");
            me.insertCombo.disabled = true;
        }

    },

    comboQtyChange: function(newValue) {
        var me = this;
        var fullComboComplete = true;
        if (newValue > 999999999) {
            isc.say(OB.I18N.getLabel('SSRCM_MAX_QUANTITY'));
        }
        if (me.productComboForm.getValue('combo')) {
            var familyIds = Object.keys(me.families);
            for (var i = 0; i < familyIds.length; i++) {
                var family = me.families[familyIds[i]];
                family.newRequiredQty = family.requiredQty * Math.floor(newValue);
                me.hLayoutInternal.getMember(1).setTabTitle(me.tabs[i], family.name + ' ' + family.alreadyFilledQty + '/' + family.newRequiredQty);

                fullComboComplete = fullComboComplete && (family.alreadyFilledQty === family.newRequiredQty);
            }
        }

        if (fullComboComplete) {
            me.insertCombo.setState("");
            me.insertCombo.disabled = false;
        } else {
            me.insertCombo.setState("Disabled");
            me.insertCombo.disabled = true;
        }
    },

    initWidget: function() {

        this.addProduct = isc.OBFormButton.create({
            title: OB.I18N.getLabel('SSRCM_add_product'),
            parent: this,
            disabled: true,
            action: function() {
                var parent = this.parent;
                parent.viewGrid.deselectAllRecords();
                parent.vLayoutForms.removeMember(parent.vLayoutForms.getMembers());


                parent.productComboForm = isc.OBViewForm.create({
                    width: '80%',
                    heigth: '100%',
                    numCols: 8,
                    itemChange: function(item, newValue) {

                        if (item.valueMap && item.valueMap[newValue]) {
                            parent.getProductPriceAndStock(parent.parentId, newValue)
                        } else if (item.columnName === "Qtyordered") {

                            parent.changeQtyOrdered(newValue);
                        } /*else if (item.columnName === "Description") {
                            //me.insertProduct.disabled = false;
                            me.insertProduct.setState("");
                        }*/

                    },

                    fields: SSRCM.fieldsFormProduct
                });
                parent.vLayoutForms.addMember(parent.productComboForm);
                parent.insertProduct.title = OB.I18N.getLabel('SSRCM_add_product');
                parent.vLayoutForms.addMember(parent.insertProduct);
                parent.insertProduct.setState("Disabled");
                parent.insertProduct.disabled = true;

                parent.deleteLine.setState("Disabled");
                parent.deleteLine.disabled = true;
                parent.setProducts();
            }
        });

        this.insertProduct = isc.OBFormButton.create({
            title: OB.I18N.getLabel('SSRCM_insert_product'),
            parent: this,
            disabled: true,
            action: function() {

                var parent = this.parent;
                parent.vLayoutForms.removeMember(parent.vLayoutForms.getMembers());
                parent.insertProductLine();

                parent.deleteLine.setState("Disabled");
                parent.deleteLine.disabled = true;
            }
        });

        this.addCombo = isc.OBFormButton.create({
            title: OB.I18N.getLabel('SSRCM_add_combo'),
            parent: this,
            disabled: true,

            action: function() {

                var parent = this.parent;

                parent.comboLines = null;
                parent.selecteLineId = null;
                parent.viewGrid.deselectAllRecords();
                parent.vLayoutForms.removeMember(parent.vLayoutForms.getMembers());


                parent.hLayoutInternal = isc.HLayout.create({
                    // showEdges: true,
                    width: '100%',
                    height: '100%'
                });
                var vLayoutInternal = isc.VLayout.create({
                    width: '140px',
                    height: '100%',
                    defaultLayoutAlign: 'center',
                    align: 'center',
                    membersMargin: 10
                });
                parent.productComboForm = isc.OBViewForm.create({
                    membersMargin: 10,
                    itemChange: function(item, newValue) {
                        if (item.valueMap && item.valueMap[newValue]) {
                            parent.loadFamilies(newValue);
                        }
                        if (item.editorType === "spinner") {
                            parent.comboQtyChange(newValue);
                        }
                    },
                    fields: [{
                        titleAlign: 'top',
                        name: 'combo',
                        title: OB.I18N.getLabel('SSRCM_Combo'),
                        required: true,
                        canFilter: true,
                        type: '_id_CB6C648A6CAB4F749CB25EB1EBC4BAB5',
                        valueMap: parent.comboList
                    }, {
                        name: '',
                        type: 'spacer'
                    }, {
                        colspan: 2,
                        title: "Quantity",
                        name: "quantity",
                        showTitle: false,
                        editorType: "spinner",
                        writeStackedIcons: false,
                        defaultValue: 1,
                        min: 1,
                        step: 1
                    }]
                });

                vLayoutInternal.addMember(parent.productComboForm);
                vLayoutInternal.addMember(parent.insertCombo);
                parent.insertCombo.title = "Insertar Combo";
                parent.hLayoutInternal.addMember(vLayoutInternal);
                parent.vLayoutForms.addMember(parent.hLayoutInternal);

                // parent.vLayoutForms.addMember(parent.insertCombo);
                parent.insertCombo.setState("Disabled");
                parent.insertCombo.disabled = true;


                parent.deleteLine.setState("Disabled");
                parent.deleteLine.disabled = true;
                parent.setCombos();
            }
        });

        this.insertCombo = isc.OBFormButton.create({
            title: OB.I18N.getLabel('SSRCM_insert_combo'),
            parent: this,
            disabled: false,
            action: function() {
                var parent = this.parent;
                parent.vLayoutForms.removeMember(parent.vLayoutForms.getMembers());
                parent.insertComboLine();

                parent.deleteLine.setState("Disabled");
                parent.deleteLine.disabled = true;
            }
        });

        this.deleteLine = isc.OBFormButton.create({
            title: OB.I18N.getLabel('SSRCM_delete_line'),
            parent: this,
            disabled: true,
            action: function() {
                var parent = this.parent;

                parent.vLayoutForms.removeMember(parent.vLayoutForms.getMembers());
                parent.insertProduct.setState("Disabled");
                parent.insertProduct.disabled = true;
                parent.deleteLineAction();
            }
        });

        this.toolBar = isc.OBToolbar.create({
            view: this,
            visibility: 'hidden',
            leftMembers: [],
            rightMembers: []
        });

        this.viewGrid = isc.SSRCM_Lines_Grid.create({
            parent: this
        });

        var vLayout = isc.VLayout.create({
            // showEdges: true,
            width: '100%'
        });

        this.hLayout = isc.HLayout.create({
            // showEdges: true,
            width: '100%',
            height: '40%'
        });

        this.vLayoutGrid = isc.VLayout.create({
            // showEdges: true,
            width: '100%',
            height: '60%'
        });

        var vLayoutButtons = isc.VLayout.create({
            // showEdges: true,
            width: '20%',
            heigth: '100%',
            defaultLayoutAlign: 'center',
            align: 'center',
            membersMargin: 10
        });

        this.vLayoutForms = isc.VLayout.create({
            // showEdges: true,
            width: '100%',
            heigth: '95%',
            defaultLayoutAlign: 'center',
            align: 'center',
            membersMargin: 10,
            layoutBottomMargin: 10,
            layoutTopMargin: 10
        });

        this.productComboForm = isc.OBViewForm.create({
            // showEdges: true,
            width: '80%',
            heigth: '100%',
            numCols: 4,
            fields: []
        });

        vLayoutButtons.addMember(this.addProduct);
        vLayoutButtons.addMember(this.addCombo);
        vLayoutButtons.addMember(this.deleteLine);

        this.vLayoutGrid.addMember(this.viewGrid);
        this.hLayout.addMember(vLayoutButtons);

        this.vLayoutForms.addMember(this.productComboForm);
        // this.vLayoutForms.addMember(insertProduct);

        this.hLayout.addMember(this.vLayoutForms);

        vLayout.addMember(this.hLayout);
        vLayout.addMember(this.vLayoutGrid);
        this.addMember(vLayout);
        this.Super('initWidget', arguments);
    },

    // the following three methods are related to the view handling
    // api of Openbravo
    isSameTab: function(viewId, params) {
        return viewId === this.getClassName();
    },

    // just return the classname and nothing else to be bookmarked
    getBookMarkParams: function() {
        var result = {};
        result.viewId = this.getClassName();
        return result;
    },

    // this view does not have a help view
    getHelpView: function() {
        return;
    }
});