/*jslint*/
OB.Layout.ViewManager.loadedWindowClassName = '_D38B368614934291BD5746ACF0C65EED_1539338550408';
isc.ClassFactory.defineClass('SSRCM_CreateOrders', isc.OBStandardWindow).addProperties({
    windowId: 'D38B368614934291BD5746ACF0C65EED',
    multiDocumentEnabled: false,
    viewProperties: {
        windowId: 'D38B368614934291BD5746ACF0C65EED',
        standardWindow: this.standardWindow,

        tabTitle: OB.I18N.getLabel('SSRCM_CallCenterOrder'),
        entity: 'saqb_order',
        isDeleteableTable: true,
        tabId: 'B5872828CEAE42BEB2A134A535D01F19',
        moduleId: '9944FED4BC3B45D6AC9A1BF6F5ADABB8',



        mapping250: '/ec.com.sidesoft.quickbilling.advancedCallCenterOrder/CallCenterOrderB5872828CEAE42BEB2A134A535D01F19',

        standardProperties: {
            inpTabId: 'B5872828CEAE42BEB2A134A535D01F19',
            inpwindowId: 'D38B368614934291BD5746ACF0C65EED',
            inpTableId: '94EBF5932FB64B889BCF89072D9BD625',
            inpkeyColumnId: 'Saqb_Order_ID',
            keyProperty: 'id',
            inpKeyName: 'inpsaqbOrderId',
            keyColumnName: 'Saqb_Order_ID',
            keyPropertyType: '_id_13'
        },

        actionToolbarButtons: [{
            id: 'EEB9372D6AF64CDCA6386B3F1E52C080',
            title: OB.I18N.getLabel('SSRCM_CallCenterOrderPost'),
            obManualURL: '/ec.com.sidesoft.quickbilling.advancedCallCenterOrder/CallCenterOrderB5872828CEAE42BEB2A134A535D01F19_Edition.html',
            command: 'BUTTONProcessD508034AB4D94C948E7B3407DF12D71C',
            property: 'process',
            processId: 'D508034AB4D94C948E7B3407DF12D71C',
            modal: false,
            displayIf: function(form, currentValues, context) {
                return (OB.Utilities.getValue(currentValues, 'documentStatus') === 'DR'
                 && 
                //         OB.Utilities.getValue(currentValues, 'saqbOrdertype') != 'pup') &&
                        OB.Utilities.getValue(currentValues, 'saqbApprovetrx') != true);
            },
            autosave: true
        }],

        showParentButtons: true,

        buttonsHaveSessionLogic: false,

        fields: [
            /*{
                    name: 'B8E784CDBCDC486FA3719D35441CA1F3',
                    title: OB.I18N.getLabel('SSRCM_Partner|'), //Cliente
                    sectionExpanded: true,
                    defaultValue: OB.I18N.getLabel('SSRCM_Partner|'), //Cliente
                    itemIds: ['sSWHTypeID', 'orderDate', 'cIFNif', 'namePartner', 'email','description','documentNo'],
                    type: 'OBSectionItem'
                  },*/
            {
                name: 'orderDate',
                id: 'B9FF38C75D8C488CB2D272129AA9CBFC',
                title: OB.I18N.getLabel('SSRCM_OrderDate'), // 'Fecha de órden',
                required: true,
                disabled: true,
                hasDefaultValue: true,
                columnName: 'Dateordered',
                inpColumnName: 'inpdateordered',
                "length": 19,
                gridProps: {
                    sort: 2,
                    cellAlign: 'left',
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true,
                    filterOnKeypress: false
                },
                type: '_id_15'
            },
            {
                name: 'sSWHTypeID',
                id: 'BC93988500904084BD49C917A72930F0',
                title: OB.I18N.getLabel('SSRCM_TypeID'), //Tipo de identificación
                required: true,
                hasDefaultValue: true,
                columnName: 'Sswh_Taxidtype',
                inpColumnName: 'inpsswhTaxidtype',
                gridProps: {
                    sort: 3,
                    length: 60,
                    showIf: 'false',
                    displaylength: 60,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true,
                    filterOnKeypress: false
                },
                type: '_id_DCF62925DDB84921955D3390BA35E72A'
            },

            {
                name: 'cIFNif',
                id: 'AB9016F41A4A45118B70E6F2A05F5BDB',
                title: OB.I18N.getLabel('SSRCM_CIFNIF'), // '# Identificación',
                required: true,
                firstFocusedField: true,
                columnName: 'CIF_Nif',
                inpColumnName: 'inpcifNif',
                "length": 40,
                gridProps: {
                    sort: 5,
                    autoExpand: true,
                    length: 40,
                    displaylength: 1,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_10'
            },
            {
                name: 'namePartner',
                id: 'D9CCA80E73E140629029F35AFB668BE2',
                title: OB.I18N.getLabel('SSRCM_Name'), // 'Nombre',
                required: true,
                columnName: 'Name_Partner',
                inpColumnName: 'inpnamePartner',
                "length": 60,
                gridProps: {
                    sort: 6,
                    autoExpand: true,
                    length: 60,
                    displaylength: 60,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_10'
            },
            {
                name: 'email',
                id: '1EC060B371D241B1B4D95FFA02653D73',
                title: OB.I18N.getLabel('SSRCM_Email'), // 'Email',
                required: true,
                columnName: 'Email',
                inpColumnName: 'inpemail',
                "length": 60,
                gridProps: {
                    sort: 7,
                    autoExpand: true,
                    length: 60,
                    displaylength: 60,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_10'
            },

            {
                name: 'documentNo',
                id: '138221F8F6E54A8C805743046F34E63C',
                title: OB.I18N.getLabel('SSRCM_Documentno'), // 'Nº documento',
                disabled: true,
                columnName: 'Documentno',
                inpColumnName: 'inpdocumentno',
                "length": 30,
                gridProps: {
                    sort: 3,
                    length: 30,
                    displaylength: 30,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_10'
            },
            {
                name: 'description',
                id: '6A4188AC3B374E08BA1BE47EE17D2A63',
                title: OB.I18N.getLabel('SSRCM_Observations'), //Observaciones
                colSpan: 1,
                rowSpan: 2,
                columnName: 'Description',
                inpColumnName: 'inpdescription',
                "length": 105,
                gridProps: {
                    sort: 7,
                    autoExpand: true,
                    showIf: 'false',
                    length: 105,
                    displaylength: 105,
                    editorType: 'OBPopUpTextAreaItem',
                    selectOnClick: true,
                    canFilter: true,
                    showHover: true,
                    canSort: false
                },
                type: '_id_14'
            },
            {
                name: 'deliverycontactName',
                id: '4840DF72C1264931AD2E216853ACB319',
                title: OB.I18N.getLabel('SSRCM_DelContact_Name'), //Nombre contacto de entrega
                columnName: 'Deliverycontact_Name',
                inpColumnName: 'inpdeliverycontactName',
                "length": 25,
                gridProps: {
                    sort: 22,
                    autoExpand: true,
                    showIf: 'false',
                    length: 25,
                    displaylength: 25,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_10'
            },
            {
                name: 'salesRegion',
                id: 'E723D2F0D57B412E9DBD01C6AAD725BC',
                title: OB.I18N.getLabel('SSRCM_SalesRegion'), // 'Zona de venta',
                required: true,
                columnName: 'C_Salesregion_ID',
                inpColumnName: 'inpcSalesregionId',
                refColumnName: 'C_SalesRegion_ID',
                targetEntity: 'SalesRegion',
                gridProps: {
                    sort: 12,
                    autoExpand: true,
                    editorProps: {
                        displayField: '_identifier',
                        valueField: 'id'
                    },
                    displaylength: 32,
                    fkField: true,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_19'
            },
            {
                name: 'orgRegion',
                id: '29C5F21EB682430E97EE412E23BF0A2C',
                title: OB.I18N.getLabel('SSRCM_Organization'),
                required: true,
                columnName: 'AD_Org_Region_ID',
                inpColumnName: 'inpadOrgRegionId',
                refColumnName: 'AD_Org_ID',
                targetEntity: 'Organization',
                gridProps: {
                    sort: 1,
                    autoExpand: true,
                    /* showIf: 'false',*/
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
                    displayProperty: 'name'
                },
                type: '_id_276'
            },
            {
                name: 'deliverycontactPhone',
                id: '349E770052184F899913B323BD7B6EC9',
                title: OB.I18N.getLabel('SSRCM_DelContact_Phone'), // Teléfono contacto de entrega
                columnName: 'Deliverycontact_Phone',
                inpColumnName: 'inpdeliverycontactPhone',
                "length": 10,
                required: true,
                gridProps: {
                    sort: 23,
                    autoExpand: true,
                    showIf: 'false',
                    length: 10,
                    displaylength: 10,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_10'
            },
            {
                name: 'attentionHours',
                id: 'A79C18974C684BD8ADB3B7F1A50A9C12',
                title: OB.I18N.getLabel('SSRCM_AttentionHours'),
                disabled: true,
                colSpan: 1,
                columnName: 'Attention_Hours',
                inpColumnName: 'inpattentionHours',
                "length": 250,
                gridProps: {
                    sort: 16,
                    autoExpand: true,
                    showIf: 'false',
                    length: 250,
                    displaylength: 60,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_10'
            },
            {
                name: 'deliveryInvoiceAddress',
                id: '425FE675DCF24EFFB0274CBC7947C56C',
                title: OB.I18N.getLabel('SSRCM_DeliveryInvoiceAddress'), //Dirección facturación y entrega
                hasDefaultValue: true,
                columnName: 'Delivery_Invoice_Address',
                inpColumnName: 'inpdeliveryInvoiceAddress',
                redrawOnChange: true,
                "width": 1,
                "overflow": "visible",
                gridProps: {
                    sort: 31,
                    showIf: 'false',
                    editorProps: {
                        showTitle: false,
                        showLabel: false
                    },
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true,
                    canGroupBy: false,
                    width: '*',
                    autoFitWidth: false,
                    yesNo: true
                },
                type: '_id_20'
            },
            {
            	name:'authorizationCode',
            	id:'FE174EFE231E4599A48E8EDCC067BA42',
            	title: OB.I18N.getLabel('Saqb_Authorization_code'), //Código de autorización
            	colSpan:1,
            	columnName:'Authorization_Code',
            	inpColumnName:'inpauthorizationCode',
            	"length":100,
            	gridProps:{
            	   sort:31,
            	   autoExpand:true,
            	   showIf:'false',
            	   length:100,
            	   displaylength:100,
            	   selectOnClick:true,
            	   canSort:true,
            	   canFilter:true,
            	   showHover:true
               },
               type:'_id_10'
            },
            {
            	name:'lONLat',
            	id:'82C49002748645938B2A12144D8D08F7',
            	title: OB.I18N.getLabel('Saqb_lonlat'), //Código de autorización
            	colSpan:1,
            	columnName:'LON_Lat',
            	inpColumnName:'inplONLat',
                required:true,
            	"length":100,
            	gridProps:{
            	   sort:31,
            	   autoExpand:true,
            	   showIf:'false',
            	   length:100,
            	   displaylength:100,
            	   selectOnClick:true,
            	   canSort:true,
            	   canFilter:true,
            	   showHover:true
               },
               type:'_id_10'
            },
            {
                name: 'saqbOrdertype',
                id: 'E12FB7ADA35F407BA27358633BE4642E',
                title: OB.I18N.getLabel('Saqb_Ordertype'), // 'Tipo de orden',
                required: false,
                columnName: 'Saqb_Ordertype',
                inpColumnName: 'inpsaqbOrdertype',
                changed: function(form, item, value) {
                    form.redraw(); // esto fuerza a evaluar todos los showIf nuevamente
                },
                gridProps: {
                    sort: 30,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_17'
            },
            {
                name: 'saqbDatetimepickup',
                id: '1C5CED8FB74B4E52BB2F42242A5DE5E0',
                title: OB.I18N.getLabel('SAQB_Datetimepickup'), // 'Fecha de preparacion',
                required: false,
                hasDefaultValue: true,
                columnName: 'Saqb_Datetimepickup',
                inpColumnName: 'inpsaqbDatetimepickup',
                "length": 25,
                showIf: function(item, value, form, values) {
                    return values.saqbOrdertype === 'pup';
                },
                // displayIf: function(form, currentValues, context) {
                //     return (OB.Utilities.getValue(currentValues, 'saqbOrdertype') === 'pup');
                // },
                gridProps: {
                    sort: 2,
                    cellAlign: 'left',
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true,
                    filterOnKeypress: false
                },
                type: '_id_16'
            },
            // {
            //     name: '',
            //     personalizable: false,
            //     type: 'spacer'
            // },
            /*{
        name: '000AC48553D4427D951461D593AE711F',
        title: OB.I18N.getLabel('SSRCM_Address'),// 'Dirección',
        sectionExpanded: true,
        defaultValue: OB.I18N.getLabel('SSRCM_Address'),// 'Dirección',
        itemIds: ['address1AliasRef','addressComplete', 'phone', 'address1', 'address1Alias', 'salesRegion', 'orgRegion', 'nEWAddress','attentionHours'],
        type: 'OBSectionItem'
      },*/
            {
                name: 'address1AliasRef',
                id: 'DC07E45A36064E56BD27F6C29D0456BB',
                title: OB.I18N.getLabel('SSRCM_Address'), // 'Dirección',
                columnName: 'Address1_Alias_Ref',
                inpColumnName: 'inpaddress1AliasRef',
                refColumnName: 'C_BPartner_Location_ID',
                targetEntity: 'BusinessPartnerLocation',
                gridProps: {
                    sort: 8,
                    autoExpand: true,
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
                    displayProperty: 'saqbAlias'
                },
                type: '_id_8257133339914829A33CF3848297B6A7'
            },
            {
                name: 'nEWAddress',
                id: '5E2EAD1AD8B84AECAB7E5E9E2B428906',
                title: OB.I18N.getLabel('SSRCM_NewAddress'), // 'Nueva dirección',
                columnName: 'NEW_Address',
                inpColumnName: 'inpnewAddress',
                redrawOnChange: true,
                "width": 1,
                "overflow": "visible",
                gridProps: {
                    sort: 9,
                    editorProps: {
                        showTitle: false,
                        showLabel: false
                    },
                    selectOnClick: true,
                    showIf: 'false',
                    canSort: true,
                    canFilter: true,
                    showHover: true,
                    canGroupBy: false,
                    width: '*',
                    autoFitWidth: false,
                    yesNo: true
                },
                type: '_id_20'
            },
            {
                name: 'address1AliasRefFac',
                id: 'E0B305623B8D41D280B0B770CB613DEB',
                title: OB.I18N.getLabel('SSRCM_AddressFac'), // 'Dirección facturación',
                columnName: 'Address1_Alias_Ref_Fac',
                inpColumnName: 'inpaddress1AliasRefFac',
                refColumnName: 'C_BPartner_Location_ID',
                targetEntity: 'BusinessPartnerLocation',
                showIf: function(item, value, form, currentValues, context) {
                    return (OB.Utilities.getValue(currentValues, 'deliveryInvoiceAddress') === false);
                },
                gridProps: {
                    sort: 27,
                    autoExpand: true,
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
                    criteriaField: 'address1AliasRefFac$saqbAlias',
                    criteriaDisplayField: 'saqbAlias',
                    displayProperty: 'saqbAlias'
                },
                type: '_id_8257133339914829A33CF3848297B6A7'
            },
            {
                name: 'nEWAddressFac',
                id: '91662E957CCC4635B76592548FE69858',
                title: OB.I18N.getLabel('SSRCM_NewAddress_Fac'), // 'Nueva dirección facturación',
                columnName: 'NEW_Address_Fac',
                inpColumnName: 'inpnewAddressFac',
                showIf: function(item, value, form, currentValues, context) {
                    return (OB.Utilities.getValue(currentValues, 'deliveryInvoiceAddress') === false);
                },
                "width": 1,
                "overflow": "visible",
                gridProps: {
                    sort: 24,
                    showIf: 'false',
                    editorProps: {
                        showTitle: false,
                        showLabel: false
                    },
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true,
                    canGroupBy: false,
                    width: '*',
                    autoFitWidth: false,
                    yesNo: true
                },
                type: '_id_20'
            },
            {
                name: 'addressComplete',
                id: '4F9DA55183EC426BAFF624C0C653FBCB',
                title: OB.I18N.getLabel('SSRCM_Address_Complete'), //'Dirección completa',
                columnName: 'address_complete',
                inpColumnName: 'inpaddressComplete',
                "length": 60,
                required: true,
                gridProps: {
                    sort: 7,
                    autoExpand: true,
                    length: 60,
                    displaylength: 60,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_10'
            },
            {
                name: 'phone',
                id: 'A6B15D0C73F343C6A1358427A8E1F584',
                title: OB.I18N.getLabel('SSRCM_Phone'), // 'Teléfono',
                columnName: 'Phone',
                inpColumnName: 'inpphone',
                required: true,
                "length": 25,
                gridProps: {
                    sort: 11,
                    autoExpand: true,
                    showIf: 'false',
                    length: 25,
                    displaylength: 25,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_10'
            },
            {
                name: 'addressCompleteFac',
                id: '78992105DAE14282A2755EBF4D5710AB',
                title: OB.I18N.getLabel('SSRCM_Address_Complete_Fac'), //'Dirección completa facturación',
                columnName: 'Address_Complete_Fac',
                inpColumnName: 'inpaddressCompleteFac',
                showIf: function(item, value, form, currentValues, context) {
                    return (OB.Utilities.getValue(currentValues, 'deliveryInvoiceAddress') === false);
                },
                "length": 60,
                gridProps: {
                    sort: 26,
                    autoExpand: true,
                    showIf: 'false',
                    length: 60,
                    displaylength: 60,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_10'
            },
            {
                name: 'phoneFac',
                id: '2751289175B242028022AA749E7B49CB',
                title: OB.I18N.getLabel('SSRCM_Phone_Fac'), // 'Teléfono facturación',
                columnName: 'Phone_Fac',
                inpColumnName: 'inpphoneFac',
                showIf: function(item, value, form, currentValues, context) {
                    return (OB.Utilities.getValue(currentValues, 'deliveryInvoiceAddress') === false);
                },
                "length": 25,
                gridProps: {
                    sort: 28,
                    autoExpand: true,
                    showIf: 'false',
                    length: 25,
                    displaylength: 25,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_10'
            },
            {
                name: 'address1',
                id: '610AD95E30F44957867A5BB28F625427',
                title: OB.I18N.getLabel('SSRCM_Reference'), //Referencia
                columnName: 'Address1',
                inpColumnName: 'inpaddress1',
                required: true,
                "length": 105,
                gridProps: {
                    sort: 14,
                    autoExpand: true,
                    length: 105,
                    displaylength: 105,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_14'
            },
            {
                name: 'address1Alias',
                id: 'DA6A0D58392943D6902790350649FC0E',
                title: OB.I18N.getLabel('SSRCM_AddressAlias'), //Alias
                inpColumnName: 'inpaddress1Alias',
                columnName: 'Address1_Alias',
                required: true,
                "length": 60,
                gridProps: {
                    sort: 15,
                    autoExpand: true,
                    length: 60,
                    displaylength: 60,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_10'
            },

         /*   {
                name: 'address1Fac',
                id: 'F6BDECF5612445B1A28AFC87A11DFB68',
                title: OB.I18N.getLabel('SSRCM_Reference_Fac'), //Referencia facturación
                colSpan: 1,
                rowSpan: 2,
                columnName: 'Address1_Fac',
                inpColumnName: 'inpaddress1Fac',
                showIf: function(item, value, form, currentValues, context) {
                    return (OB.Utilities.getValue(currentValues, 'deliveryInvoiceAddress') === false);
                },
                "length": 105,
                gridProps: {
                    sort: 29,
                    autoExpand: true,
                    showIf: 'false',
                    length: 105,
                    displaylength: 105,
                    editorType: 'OBPopUpTextAreaItem',
                    selectOnClick: true,
                    canFilter: true,
                    showHover: true,
                    canSort: false
                },
                type: '_id_14'
            },*/
            {
                name: 'address1AliasFac',
                id: '424C541C764848E7978FB1BD39F35B19',
                title: OB.I18N.getLabel('SSRCM_AddressAlias_Fac'), //Alias facturación
                columnName: 'Address1_Alias_Fac',
                inpColumnName: 'inpaddress1AliasFac',
                showIf: function(item, value, form, currentValues, context) {
                    return (OB.Utilities.getValue(currentValues, 'deliveryInvoiceAddress') === false);
                },
                "length": 60,
                gridProps: {
                    sort: 30,
                    autoExpand: true,
                    showIf: 'false',
                    length: 60,
                    displaylength: 60,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_10'
            },
            {
                name: 'bpartnerLocationFac',
                id: 'CEEC219A2B8C4E2498B7A2A693C1D712',
                title: 'C_Bpartner_Location_Fac_ID',
                columnName: 'C_Bpartner_Location_Fac_ID',
                inpColumnName: 'inpcBpartnerLocationFacId',
                refColumnName: 'C_BPartner_Location_ID',
                targetEntity: 'BusinessPartnerLocation',
                showIf: 'false',
                type: '_id_8257133339914829A33CF3848297B6A7'
            },
            {
                name: 'defaultproductscharged',
                id: '8373C8D9EBCF4358A9574BC7938F3FE7',
                title: 'Defaultproductscharged',
                hasDefaultValue: true,
                columnName: 'Defaultproductscharged',
                inpColumnName: 'inpdefaultproductscharged',
                "width": 1,
                "overflow": "visible",
                showIf: 'false',
                type: '_id_20'
            },
            {
                name: '1000100001',
                title: OB.I18N.getLabel('SSRCM_Audit'), // 'Audit',
                personalizable: false,
                defaultValue: OB.I18N.getLabel('SSRCM_Audit'),
                itemIds: ['creationDate', 'createdBy', 'updated', 'updatedBy'],
                type: 'OBAuditSectionItem'
            },
            {
                name: 'creationDate',
                title: OB.I18N.getLabel('SSRCM_CreationDate'), // 'Creado',
                disabled: true,
                updatable: false,
                personalizable: false,
                gridProps: {
                    sort: 990,
                    cellAlign: 'left',
                    showIf: 'false',
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_16'
            },
            {
                name: 'createdBy',
                title: OB.I18N.getLabel('SSRCM_CreatedBy'), // 'Creado por',
                disabled: true,
                updatable: false,
                personalizable: false,
                targetEntity: 'User',
                displayField: 'createdBy$_identifier',
                gridProps: {
                    sort: 990,
                    cellAlign: 'left',
                    showIf: 'false',
                    fkField: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_30'
            },
            {
                name: 'updated',
                title: OB.I18N.getLabel('SSRCM_Updated'), // 'Actualizado',
                disabled: true,
                updatable: false,
                personalizable: false,
                gridProps: {
                    sort: 990,
                    cellAlign: 'left',
                    showIf: 'false',
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_16'
            },
            {
                name: 'updatedBy',
                title: OB.I18N.getLabel('SSRCM_UpdatedBy'), // 'Actualizado por',
                disabled: true,
                updatable: false,
                personalizable: false,
                targetEntity: 'User',
                displayField: 'updatedBy$_identifier',
                gridProps: {
                    sort: 990,
                    cellAlign: 'left',
                    showIf: 'false',
                    fkField: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true
                },
                type: '_id_30'
            },
            {
                name: '_notes_',
                personalizable: false,
                type: 'OBNoteSectionItem'
            },
            {
                name: '_notes_Canvas',
                personalizable: false,
                type: 'OBNoteCanvasItem'
            },
            {
                name: '_linkedItems_',
                personalizable: false,
                type: 'OBLinkedItemSectionItem'
            },
            {
                name: '_linkedItems_Canvas',
                personalizable: false,
                type: 'OBLinkedItemCanvasItem'
            },
            {
                name: '_attachments_',
                personalizable: false,
                type: 'OBAttachmentsSectionItem'
            },
            {
                name: '_attachments_Canvas',
                personalizable: false,
                type: 'OBAttachmentCanvasItem'
            },
            {
                name: 'documentStatus',
                title: OB.I18N.getLabel('SSRCM_DocumentStatus'), // 'Document Status',
                required: true,
                disabled: true,
                hasDefaultValue: true,
                columnName: 'Docstatus',
                inpColumnName: 'inpdocstatus',
                displayed: false,
                gridProps: {
                    sort: 25,
                    length: 60,
                    displaylength: 60,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true,
                    filterOnKeypress: false
                },
                type: '_id_5F04F7ABF7A04316A2F233B63855A419'
            },
            {
                name: 'summedLineAmount',
                title: OB.I18N.getLabel('SSRCM_Subtotal'), //Subtotal
                required: true,
                disabled: true,
                hasDefaultValue: true,
                columnName: 'Totallines',
                inpColumnName: 'inptotallines',
                displayed: false,
                gridProps: {
                    sort: 26,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true,
                    filterOnKeypress: false
                },
                type: '_id_12'
            },
            {
                name: 'grandTotalAmount',
                title: OB.I18N.getLabel('SSRCM_GrandTotalAmount'), // 'Grand Total Amount',
                required: true,
                disabled: true,
                hasDefaultValue: true,
                columnName: 'Grandtotal',
                inpColumnName: 'inpgrandtotal',
                displayed: false,
                gridProps: {
                    sort: 27,
                    selectOnClick: true,
                    canSort: true,
                    canFilter: true,
                    showHover: true,
                    filterOnKeypress: false
                },
                type: '_id_12'
            }


        ],

        statusBarFields: [
            'documentStatus', 'summedLineAmount', 'grandTotalAmount'
        ],

        initialPropertyToColumns: [{
            property: 'organization',
            inpColumn: 'inpadOrgId',
            dbColumn: 'AD_Org_ID',
            sessionProperty: true,
            type: '_id_19'
        }, {
            property: 'businessPartner',
            inpColumn: 'inpcBpartnerId',
            dbColumn: 'C_Bpartner_ID',
            type: '_id_800057'
        }, {
            property: 'partnerAddress',
            inpColumn: 'inpcBpartnerLocationId',
            dbColumn: 'C_Bpartner_Location_ID',
            type: '_id_19'
        }, {
            property: 'process',
            inpColumn: 'inpprocess',
            dbColumn: 'Process',
            type: '_id_28'
        }, {
            property: 'partnerAddress',
            inpColumn: 'inpcBpartnerLocationId',
            dbColumn: 'C_Bpartner_Location_ID',
            type: '_id_19'
        }, {
            property: 'id',
            inpColumn: 'inpsaqbOrderId',
            dbColumn: 'Saqb_Order_ID',
            type: '_id_13'
        }, {
            property: 'client',
            inpColumn: 'inpadClientId',
            dbColumn: 'AD_Client_ID',
            sessionProperty: true,
            type: '_id_19'
        }, {
            property: 'id',
            inpColumn: 'Saqb_Order_ID',
            dbColumn: 'Saqb_Order_ID',
            sessionProperty: true,
            type: '_id_13'
        }],

        iconToolbarButtons: [{
            action: function() {
                OB.ToolbarUtils.showAuditTrail(this.view);
            },
            buttonType: 'audit',
            prompt: 'Show Audit Trail'
        }],

        hasChildTabs: true,
        createViewStructure: function() {
            this.addChildView(
                isc.SSRCM_Lines.create({
                    hasChildTabs: false,
                    tabTitle: OB.I18N.getLabel('SSRCM_LinesCallCenter'), // 'Lines Call Center',
                    entity: 'saqb_orderline',
                    parentProperty: 'saqbOrder',
                    moduleId: '9944FED4BC3B45D6AC9A1BF6F5ADABB8',
                    standardWindow: this.standardWindow
                }));

            //INICIO SOLAPA PAGOS

            this.addChildView(isc.OBStandardView.create({
                standardWindow: this.standardWindow,
                tabTitle: OB.I18N.getLabel('SSRCM_PaymentMethods'),
                entity: 'saqbp_payment_methods',
                isDeleteableTable: true,
                tabId: 'CD952316A3EF40D69252FDB9750EFCD4',
                parentProperty: 'saqbOrder',
                moduleId: '6F631B136F274F8FAF8E2766ADEE4B2B',
                mapping250: '/ec.com.sidesoft.quickbilling.advancedCallCenterOrder/PaymentMethodsCD952316A3EF40D69252FDB9750EFCD4',
                standardProperties: {
                    inpTabId: 'CD952316A3EF40D69252FDB9750EFCD4',
                    inpwindowId: 'D38B368614934291BD5746ACF0C65EED',
                    inpTableId: '12E4AFEE19F04D44A39CB43F37F95ECB',
                    inpkeyColumnId: 'Saqbp_Payment_Methods_ID',
                    keyProperty: 'id',
                    inpKeyName: 'inpsaqbpPaymentMethodsId',
                    keyColumnName: 'Saqbp_Payment_Methods_ID',
                    keyPropertyType: '_id_13'
                },
                actionToolbarButtons: [],
                showParentButtons: true,
                buttonsHaveSessionLogic: false,
                fields: [{
                        name: 'lineNo',
                        id: '9B74612D0B2C460AA38DFE1AACB260A2',
                        title: OB.I18N.getLabel('SSRCM_Lineno'), // Line No., 
                        required: true,
                        hasDefaultValue: true,
                        columnName: 'Line',
                        inpColumnName: 'inpline',
                        gridProps: {
                            sort: 1,
                            selectOnClick: true,
                            canSort: true,
                            canFilter: true,
                            showHover: true,
                            filterOnKeypress: false
                        },
                        type: '_id_11'
                    }, {
                        name: 'paymentMethod',
                        id: '09ED45E740514691B564E907EA732D7C',
                        title: OB.I18N.getLabel('SSRCM_PaymentMethod'), // 'Método de pago',
                        required: true,
                        columnName: 'FIN_Paymentmethod_ID',
                        inpColumnName: 'inpfinPaymentmethodId',
                        refColumnName: 'Fin_Paymentmethod_ID',
                        targetEntity: 'FIN_PaymentMethod',
                        gridProps: {
                            sort: 2,
                            autoExpand: true,
                            editorProps: {
                                displayField: '_identifier',
                                valueField: 'id'
                            },
                            displaylength: 32,
                            fkField: true,
                            selectOnClick: true,
                            canSort: true,
                            canFilter: true,
                            showHover: true
                        },
                        type: '_id_19'
                    }, {
                        name: 'amount',
                        id: '7A17C3612E3C40B5BB62A6A7A2993486',
                        title: OB.I18N.getLabel('SSRCM_Amount'), // 'Valor',
                        required: true,
                        hasDefaultValue: true,
                        columnName: 'Amount',
                        inpColumnName: 'inpamount',
                        gridProps: {
                            sort: 3,
                            selectOnClick: true,
                            canSort: true,
                            canFilter: true,
                            showHover: true,
                            filterOnKeypress: false
                        },
                        type: '_id_12'
                    },
                    {
                        name: 'billetValue',
                        id: 'D3E834ABEA0C43D58FBE115CC4BBF885',
                        title: OB.I18N.getLabel('SSRCM_BilletValue'), // 'Valor del billete',
                        columnName: 'Billet_Value',
                        inpColumnName: 'inpbilletValue',
                        showIf: function(item, value, form, currentValues, context) {
                            return (OB.Utilities.getValue(currentValues, 'typeCallCenter') === 'EFE');
                        },
                        gridProps: {
                            sort: 4,
                            showIf: 'false',
                            selectOnClick: true,
                            canSort: true,
                            canFilter: true,
                            showHover: true,
                            filterOnKeypress: false
                        },
                        type: '_id_11'
                    }, {
                        name: 'check',
                        id: '8CFBFA9842AC4F6F8A28899C520AC949',
                        title: OB.I18N.getLabel('SSRCM_NO_Check'), // 'No. de cheque',
                        columnName: 'NO_Check',
                        inpColumnName: 'inpnoCheck',
                        "length": 60,
                        showIf: function(item, value, form, currentValues, context) {
                            return (OB.Utilities.getValue(currentValues, 'typeCallCenter') === 'CHE');
                        },
                        gridProps: {
                            sort: 6,
                            autoExpand: true,
                            showIf: 'false',
                            length: 60,
                            displaylength: 60,
                            selectOnClick: true,
                            canSort: true,
                            canFilter: true,
                            showHover: true
                        },
                        type: '_id_10'
                    }, 
                    {
                        name: 'scaiCardBrand',
                        id: 'F12E26BEF5C4472787656C29023B28AB',
                        title: OB.I18N.getLabel('SSRCM_Card_Brand'),
                        columnName: 'Scai_Card_Brand_ID',
                        inpColumnName: 'inpscaiCardBrandId',
                        refColumnName: 'Scai_Card_Brand_ID',
                        targetEntity: 'scai_card_brand',
                        showIf: function(item, value, form, currentValues, context) {
                            return (OB.Utilities.getValue(currentValues, 'typeCallCenter') === 'TAR');
                        },
                        gridProps: {
                            sort: 13,
                            autoExpand: true,
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
                            showHover: true
                        },
                        type: '_id_19'
                    },
                    {
                        name: 'saqbFinancialEntity',
                        id: '6E5EEEFEDEDF490BB519BB8EBF0F7811',
                        title: OB.I18N.getLabel('SSRCM_FinancialEntity'), // 'Entidad financiera',
                        columnName: 'Saqb_Financial_Entity_ID',
                        inpColumnName: 'inpsaqbFinancialEntityId',
                        refColumnName: 'Saqb_Financial_Entity_ID',
                        targetEntity: 'saqb_financial_entity',
                        showIf: function(item, value, form, currentValues, context) {
                            return (OB.Utilities.getValue(currentValues, 'typeCallCenter') === 'CHE' || OB.Utilities.getValue(currentValues, 'typeCallCenter') === 'TAR');
                        },
                        gridProps: {
                            sort: 5,
                            autoExpand: true,
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
                            showHover: true
                        },
                        type: '_id_19'
                    },  
                    {
                        name: 'cardtype',
                        id: '27E808478D8E4F4CA823760E5A3EA003',
                        title: OB.I18N.getLabel('SSRCM_CardType'), // 'Tipo de tarjeta',
                        columnName: 'Cardtype',
                        inpColumnName: 'inpcardtype',
                        showIf: function(item, value, form, currentValues, context) {
                            return (OB.Utilities.getValue(currentValues, 'typeCallCenter') === 'TAR');
                        },
                        gridProps: {
                            sort: 10,
                            showIf: 'false',
                            length: 60,
                            displaylength: 60,
                            selectOnClick: true,
                            canSort: true,
                            canFilter: true,
                            showHover: true,
                            filterOnKeypress: false
                        },
                        type: '_id_9E53BDD29C3740618A1B00478DC46243'
                    },
                    
                    /*{
                        name: 'saqbCard',
                        id: 'DF18469D4FB74E309E2AADC3882FE157',
                        title: OB.I18N.getLabel('SSRCM_Card'), // 'Tarjeta',
                        columnName: 'Saqb_Card_ID',
                        inpColumnName: 'inpsaqbCardId',
                        refColumnName: 'Saqb_Card_ID',
                        targetEntity: 'saqb_card',
                        showIf: function(item, value, form, currentValues, context) {
                            return (OB.Utilities.getValue(currentValues, 'typeCallCenter') === 'TAR');
                        },
                        gridProps: {
                            sort: 7,
                            autoExpand: true,
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
                            showHover: true
                        },
                        type: '_id_19'
                    }, {
                        name: 'newcard',
                        id: 'E4E1FF2F205843D2A18234D4F8872FCF',
                        title: OB.I18N.getLabel('SSRCM_Newcard'), // 'Nueva tarjeta',
                        columnName: 'Newcard',
                        inpColumnName: 'inpnewcard',
                        "width": 1,
                        "overflow": "visible",
                        showIf: function(item, value, form, currentValues, context) {
                            return (OB.Utilities.getValue(currentValues, 'typeCallCenter') === 'TAR');
                        },
                        gridProps: {
                            sort: 8,
                            showIf: 'false',
                            editorProps: {
                                showTitle: false,
                                showLabel: false
                            },
                            selectOnClick: true,
                            canSort: true,
                            canFilter: true,
                            showHover: true,
                            canGroupBy: false,
                            width: '*',
                            autoFitWidth: false,
                            yesNo: true
                        },
                        type: '_id_20'
                    },*/
                    {
                        name: 'cardno',
                        id: '82C5EAE709E14E18970DA00D4F12B8F8',
                        title: OB.I18N.getLabel('SSRCM_Cardno'), // 'No. de tarjeta',
                        columnName: 'Cardno',
                        inpColumnName: 'inpcardno',
                        "length": 30,
                        showIf: function(item, value, form, currentValues, context) {
                            return (OB.Utilities.getValue(currentValues, 'typeCallCenter') === 'TAR');
                        },
                        gridProps: {
                            sort: 9,
                            autoExpand: true,
                            showIf: 'false',
                            length: 30,
                            displaylength: 30,
                            selectOnClick: true,
                            canSort: true,
                            canFilter: true,
                            showHover: true
                        },
                        type: '_id_10'
                    },                    
                    {
                        name: 'cardpropietary',
                        id: 'A618EC687E9549D0A21439F570E24DA0',
                        title: OB.I18N.getLabel('SSRCM_CardPropietary'), // 'Propietario',
                        columnName: 'Cardpropietary',
                        inpColumnName: 'inpcardpropietary',
                        "length": 60,
                        showIf: function(item, value, form, currentValues, context) {
                            return (OB.Utilities.getValue(currentValues, 'typeCallCenter') === 'TAR');
                        },
                        gridProps: {
                            sort: 14,
                            autoExpand: true,
                            showIf: 'false',
                            length: 60,
                            displaylength: 60,
                            selectOnClick: true,
                            canSort: true,
                            canFilter: true,
                            showHover: true
                        },
                        type: '_id_10'
                    }, 
                    {
                        name: 'expirationdate',
                        id: '747CCCDBC6204954AB35483E51DDFCA8',
                        title: OB.I18N.getLabel('SSRCM_ExpirationDate'), // 'Fecha de expiración',
                        hasDefaultValue: true,
                        columnName: 'Expirationdate',
                        inpColumnName: 'inpexpirationdate',
                        "length": 19,
                        showIf: function(item, value, form, currentValues, context) {
                            return (OB.Utilities.getValue(currentValues, 'typeCallCenter') === 'TAR');
                        },
                        gridProps: {
                            sort: 12,
                            cellAlign: 'left',
                            showIf: 'false',
                            selectOnClick: true,
                            canSort: true,
                            canFilter: true,
                            showHover: true,
                            filterOnKeypress: false
                        },
                        type: '_id_15'
                    },
                    {
                        name: 'securitycode',
                        id: 'C1A45B8EAF104B859336D403E06302BE',
                        title: OB.I18N.getLabel('SSRCM_SecurityCode'), // 'Código de seguridad',
                        columnName: 'Securitycode',
                        inpColumnName: 'inpsecuritycode',
                        "length": 30,
                        showIf: function(item, value, form, currentValues, context) {
                            return (OB.Utilities.getValue(currentValues, 'typeCallCenter') === 'TAR');
                        },
                        gridProps: {
                            sort: 11,
                            autoExpand: true,
                            showIf: 'false',
                            length: 30,
                            displaylength: 30,
                            selectOnClick: true,
                            canSort: true,
                            canFilter: true,
                            showHover: true
                        },
                        type: '_id_10'
                    }, 
                    {
                        name: 'description',
                        id: 'C90132372D244A45BE7D11710CF16572',
                        title: OB.I18N.getLabel('SSRCM_Observations'), //Observaciones
                        colSpan: 2,
                        columnName: 'Description',
                        inpColumnName: 'inpdescription',
                        "length": 250,
                        gridProps: {
                            sort: 15,
                            autoExpand: true,
                            showIf: 'false',
                            length: 250,
                            displaylength: 250,
                            selectOnClick: true,
                            canSort: true,
                            canFilter: true,
                            showHover: true
                        },
                        type: '_id_10'
                    },
                    {
                        name: 'typeCallCenter',
                        id: 'E45F5C4C81D745B58382A5374F5BCC26',
                        title: 'Type_Call_Center',
                        disabled: true,
                        columnName: 'Type_Call_Center',
                        inpColumnName: 'inptypeCallCenter',
                        "length": 60,
                        showIf: 'false',
                        type: '_id_10'
                    },
                    {
                        name: '1000100001',
                        title: 'Auditoría',
                        personalizable: false,
                        defaultValue: 'Auditoría',
                        itemIds: ['creationDate', 'createdBy', 'updated', 'updatedBy'],
                        type: 'OBAuditSectionItem'
                    }, {
                        name: 'creationDate',
                        title: 'Fecha Creación',
                        disabled: true,
                        updatable: false,
                        personalizable: false,
                        gridProps: {
                            sort: 990,
                            cellAlign: 'left',
                            showIf: 'false',
                            canSort: true,
                            canFilter: true,
                            showHover: true
                        },
                        type: '_id_16'
                    }, {
                        name: 'createdBy',
                        title: 'Creado por',
                        disabled: true,
                        updatable: false,
                        personalizable: false,
                        targetEntity: 'User',
                        displayField: 'createdBy$_identifier',
                        gridProps: {
                            sort: 990,
                            cellAlign: 'left',
                            showIf: 'false',
                            fkField: true,
                            canSort: true,
                            canFilter: true,
                            showHover: true
                        },
                        type: '_id_30'
                    }, {
                        name: 'updated',
                        title: 'Actualizado',
                        disabled: true,
                        updatable: false,
                        personalizable: false,
                        gridProps: {
                            sort: 990,
                            cellAlign: 'left',
                            showIf: 'false',
                            canSort: true,
                            canFilter: true,
                            showHover: true
                        },
                        type: '_id_16'
                    }, {
                        name: 'updatedBy',
                        title: 'Actualizado por',
                        disabled: true,
                        updatable: false,
                        personalizable: false,
                        targetEntity: 'User',
                        displayField: 'updatedBy$_identifier',
                        gridProps: {
                            sort: 990,
                            cellAlign: 'left',
                            showIf: 'false',
                            fkField: true,
                            canSort: true,
                            canFilter: true,
                            showHover: true
                        },
                        type: '_id_30'
                    }, {
                        name: '_notes_',
                        personalizable: false,
                        type: 'OBNoteSectionItem'
                    }, {
                        name: '_notes_Canvas',
                        personalizable: false,
                        type: 'OBNoteCanvasItem'
                    }, {
                        name: '_linkedItems_',
                        personalizable: false,
                        type: 'OBLinkedItemSectionItem'
                    }, {
                        name: '_linkedItems_Canvas',
                        personalizable: false,
                        type: 'OBLinkedItemCanvasItem'
                    }, {
                        name: '_attachments_',
                        personalizable: false,
                        type: 'OBAttachmentsSectionItem'
                    }, {
                        name: '_attachments_Canvas',
                        personalizable: false,
                        type: 'OBAttachmentCanvasItem'
                    }
                ],
                statusBarFields: [],
                initialPropertyToColumns: [{
                    property: 'organization',
                    inpColumn: 'inpadOrgId',
                    dbColumn: 'AD_Org_ID',
                    sessionProperty: true,
                    type: '_id_19'
                }, {
                    property: 'active',
                    inpColumn: 'inpisactive',
                    dbColumn: 'Isactive',
                    type: '_id_20'
                }, {
                    property: 'saqbOrder',
                    inpColumn: 'inpsaqbOrderId',
                    dbColumn: 'Saqb_Order_ID',
                    type: '_id_19'
                }, {
                    property: 'client',
                    inpColumn: 'inpadClientId',
                    dbColumn: 'AD_Client_ID',
                    sessionProperty: true,
                    type: '_id_19'
                }, {
                    property: 'id',
                    inpColumn: 'inpsaqbpPaymentMethodsId',
                    dbColumn: 'Saqbp_Payment_Methods_ID',
                    type: '_id_13'
                }, {
                    property: 'id',
                    inpColumn: 'Saqbp_Payment_Methods_ID',
                    dbColumn: 'Saqbp_Payment_Methods_ID',
                    sessionProperty: true,
                    type: '_id_13'
                }],
                iconToolbarButtons: [{
                    action: function() {
                        OB.ToolbarUtils.showAuditTrail(this.view);
                    },
                    buttonType: 'audit',
                    prompt: 'Mostrar histórico de auditoría'
                }],
                initWidget: function() {
                    this.prepareFields();
                    this.dataSource = OB.Datasource.create({
                        createClassName: 'OBViewDataSource',
                        dataURL: OB.Application.contextUrl + 'org.openbravo.service.datasource/saqbp_payment_methods',
                        requestProperties: {
                            params: {
                                Constants_IDENTIFIER: '_identifier',
                                Constants_FIELDSEPARATOR: '$',
                                _className: 'OBViewDataSource'
                            }
                        },
                        fields: [{
                            name: 'id',
                            type: '_id_13',
                            primaryKey: true
                        }, {
                            name: 'client',
                            type: '_id_19'
                        }, {
                            name: 'client$_identifier'
                        }, {
                            name: 'organization',
                            type: '_id_19'
                        }, {
                            name: 'organization$_identifier'
                        }, {
                            name: 'active',
                            type: '_id_20'
                        }, {
                            name: 'creationDate',
                            type: '_id_16'
                        }, {
                            name: 'createdBy',
                            type: '_id_30'
                        }, {
                            name: 'createdBy$_identifier'
                        }, {
                            name: 'updated',
                            type: '_id_16'
                        }, {
                            name: 'updatedBy',
                            type: '_id_30'
                        }, {
                            name: 'updatedBy$_identifier'
                        }, {
                            name: 'paymentMethod',
                            type: '_id_19'
                        }, {
                            name: 'paymentMethod$_identifier'
                        }, {
                            name: 'billetValue',
                            type: '_id_11'
                        }, {
                            name: 'saqbFinancialEntity',
                            type: '_id_19'
                        }, {
                            name: 'saqbFinancialEntity$_identifier'
                        }, {
                            name: 'check',
                            type: '_id_10'
                        }, /*{
                            name: 'newcard',
                            type: '_id_20'
                        }, {
                            name: 'saqbCard',
                            type: '_id_19'
                        }, {
                            name: 'saqbCard$_identifier'
                        },*/ {
                            name: 'cardtype',
                            type: '_id_9E53BDD29C3740618A1B00478DC46243',
                            valueMap: {
                                'C': 'Crédito',
                                'D': 'Débito'
                            }
                        }, {
                            name: 'cardno',
                            type: '_id_10'
                        }, {
                            name: 'expirationdate',
                            type: '_id_15'
                        }, {
                            name: 'securitycode',
                            type: '_id_10'
                        }, {
                            name: 'scaiCardBrand',
                            type: '_id_19'
                        }, {
                            name: 'scaiCardBrand$_identifier'
                        }, {
                            name: 'description',
                            type: '_id_10'
                        }, {
                            name: 'amount',
                            type: '_id_12'
                        }, {
                            name: 'typeCallCenter',
                            type: '_id_10'
                        }, {
                            name: 'saqbOrder',
                            type: '_id_19'
                        }, {
                            name: 'saqbOrder$_identifier'
                        }, {
                            name: 'cardpropietary',
                            type: '_id_10'
                        }, {
                            name: 'lineNo',
                            type: '_id_11'
                        }]
                    });
                    this.notesDataSource = OB.Datasource.create({
                        createClassName: '',
                        dataURL: OB.Application.contextUrl + '/org.openbravo.service.datasource/090A37D22E61FE94012E621729090048',
                        requestProperties: {
                            params: {
                                Constants_IDENTIFIER: '_identifier',
                                Constants_FIELDSEPARATOR: '$'
                            }
                        },
                        fields: [{
                            name: 'id',
                            type: '_id_13',
                            primaryKey: true
                        }, {
                            name: 'client',
                            type: '_id_19'
                        }, {
                            name: 'client$_identifier'
                        }, {
                            name: 'organization',
                            type: '_id_19'
                        }, {
                            name: 'organization$_identifier'
                        }, {
                            name: 'table',
                            type: '_id_19'
                        }, {
                            name: 'table$_identifier'
                        }, {
                            name: 'record',
                            type: '_id_10'
                        }, {
                            name: 'note',
                            type: '_id_14'
                        }, {
                            name: 'isactive',
                            type: '_id_20'
                        }, {
                            name: 'creationDate',
                            type: '_id_16'
                        }, {
                            name: 'createdBy',
                            type: '_id_30'
                        }, {
                            name: 'createdBy$_identifier'
                        }, {
                            name: 'updated',
                            type: '_id_16'
                        }, {
                            name: 'updatedBy',
                            type: '_id_30'
                        }, {
                            name: 'updatedBy$_identifier'
                        }]
                    });
                    this.dataSource.potentiallyShared = true;
                    this.viewForm = isc.OBViewForm.create(isc.clone(OB.ViewFormProperties), {});
                    this.viewGrid = isc.OBViewGrid.create({
                        view: this,
                        uiPattern: 'STD',
                        filterClause: false,
                        sortField: 'line',
                        allowSummaryFunctions: true,
                        requiredGridProperties: ['id', 'client', 'organization', 'updatedBy', 'updated', 'creationDate', 'createdBy', 'organization', 'client', 'saqbOrder'],
                        fields: this.gridFields
                    });
                    this.Super('initWidget', arguments);
                }
            }));

            //FIN SOLAPA PAGOS


        },

        initWidget: function() {
            this.prepareFields();
            this.dataSource = OB.Datasource.create({
                createClassName: 'OBViewDataSource',
                dataURL: OB.Application.contextUrl + 'org.openbravo.service.datasource/saqb_order',
                requestProperties: {
                    params: {
                        Constants_FIELDSEPARATOR: '$',
                        _className: 'OBViewDataSource',
                        Constants_IDENTIFIER: '_identifier'
                    }
                },
                fields: [{
                        name: 'id',
                        type: '_id_13',
                        primaryKey: true
                    },
                    {
                        name: 'client',
                        type: '_id_19'
                    }, {
                        name: 'client$_identifier'
                    },
                    {
                        name: 'organization',
                        type: '_id_19'
                    }, {
                        name: 'organization$_identifier'
                    },
                    {
                        name: 'active',
                        type: '_id_20'
                    },
                    {
                        name: 'creationDate',
                        type: '_id_16'
                    },
                    {
                        name: 'createdBy',
                        type: '_id_30'
                    }, {
                        name: 'createdBy$_identifier'
                    },
                    {
                        name: 'updated',
                        type: '_id_16'
                    },
                    {
                        name: 'updatedBy',
                        type: '_id_30'
                    }, {
                        name: 'updatedBy$_identifier'
                    },
                    {
                        name: 'sSWHTypeID',
                        type: '_id_DCF62925DDB84921955D3390BA35E72A',
                        valueMap: {
                            'D': 'Cédula',
                            'P': 'Pasaporte',
                            'R': 'RUC'
                        }
                    },
                    {
                        name: 'orderDate',
                        type: '_id_15'
                    },
                    {
                        name: 'cIFNif',
                        type: '_id_10'
                    },
                    {
                        name: 'businessPartner',
                        type: '_id_800057'
                    }, {
                        name: 'businessPartner$_identifier'
                    },
                    {
                        name: 'namePartner',
                        type: '_id_10'
                    },
                    {
                        name: 'documentStatus',
                        type: '_id_5F04F7ABF7A04316A2F233B63855A419',
                        valueMap: {
                            'CO': 'Completado', // OB.I18N.getLabel('SSRCM_Complete'),
                            'DR': 'Borrador' //OB.I18N.getLabel('SSRCM_Draft')
                        }
                    },
                    {
                        name: 'address1AliasRef',
                        type: '_id_8257133339914829A33CF3848297B6A7'
                    }, {
                        name: 'address1AliasRef$_identifier'
                    },
                    {
                        name: 'partnerAddress',
                        type: '_id_19'
                    }, {
                        name: 'partnerAddress$_identifier'
                    },
                    {
                        name: 'phone',
                        type: '_id_10'
                    },
                    {
                        name: 'address1',
                        type: '_id_14'
                    },
                    {
                        name: 'address1Alias',
                        type: '_id_10'
                    },
                    {
                        name: 'salesRegion',
                        type: '_id_19'
                    }, {
                        name: 'salesRegion$_identifier'
                    },
                    {
                        name: 'orgRegion',
                        type: '_id_276'
                    }, {
                        name: 'orgRegion$_identifier'
                    },
                    {
                        name: 'email',
                        type: '_id_10'
                    },
                    {
                        name: 'nEWAddress',
                        type: '_id_20'
                    },
                    {
                        name: 'process',
                        type: '_id_28'
                    },
                    {
                        name: 'summedLineAmount',
                        type: '_id_12'
                    },
                    {
                        name: 'grandTotalAmount',
                        type: '_id_12'
                    },
                    {
                        name: 'businessPartner$name',
                        type: '_id_10',
                        additional: true
                    },
                    {
                        name: 'defaultproductscharged',
                        type: '_id_20'
                    },
                    {
                        name: 'attentionHours',
                        type: '_id_10'
                    },
                    {
                        name: 'deliverycontactName',
                        type: '_id_10'
                    },
                    {
                        name: 'deliverycontactPhone',
                        type: '_id_10'
                    },
                    {
                        name: 'documentNo',
                        type: '_id_10'
                    },
                    {
                        name: 'nEWAddressFac',
                        type: '_id_20'
                    },
                    {
                        name: 'addressCompleteFac',
                        type: '_id_10'
                    },
                    {
                        name: 'address1AliasRefFac',
                        type: '_id_8257133339914829A33CF3848297B6A7'
                    }, {
                        name: 'address1AliasRefFac$_identifier'
                    },
                    {
                        name: 'phoneFac',
                        type: '_id_10'
                    },
                    /*{
                        name: 'address1Fac',
                        type: '_id_14'
                    },*/
                    {
                        name: 'address1AliasFac',
                        type: '_id_10'
                    },
                    {
                        name: 'deliveryInvoiceAddress',
                        type: '_id_20'
                    },
                    {
                       name:'authorizationCode',
                       type:'_id_10'
                    }
                ]
            })

            ;
            this.notesDataSource = OB.Datasource.create({
                createClassName: '',
                dataURL: OB.Application.contextUrl + 'org.openbravo.service.datasource/090A37D22E61FE94012E621729090048',
                requestProperties: {
                    params: {
                        Constants_FIELDSEPARATOR: '$',
                        Constants_IDENTIFIER: '_identifier'
                    }
                },
                fields: [{
                        name: 'id',
                        type: '_id_13',
                        primaryKey: true
                    },
                    {
                        name: 'client',
                        type: '_id_19'
                    }, {
                        name: 'client$_identifier'
                    },
                    {
                        name: 'organization',
                        type: '_id_19'
                    }, {
                        name: 'organization$_identifier'
                    },
                    {
                        name: 'table',
                        type: '_id_19'
                    }, {
                        name: 'table$_identifier'
                    },
                    {
                        name: 'record',
                        type: '_id_10'
                    },
                    {
                        name: 'note',
                        type: '_id_14'
                    },
                    {
                        name: 'isactive',
                        type: '_id_20'
                    },
                    {
                        name: 'creationDate',
                        type: '_id_16'
                    },
                    {
                        name: 'createdBy',
                        type: '_id_30'
                    }, {
                        name: 'createdBy$_identifier'
                    },
                    {
                        name: 'updated',
                        type: '_id_16'
                    },
                    {
                        name: 'updatedBy',
                        type: '_id_30'
                    }, {
                        name: 'updatedBy$_identifier'
                    }
                ]
            })

            ;
            this.dataSource.potentiallyShared = true;
            this.viewForm = isc.OBViewForm.create(isc.clone(OB.ViewFormProperties), {
                statusBarFields: this.statusBarFields,

                obFormProperties: {
                    onFieldChanged: function(form, item, value) {
                        var f = form || this,
                            context = this.view.getContextInfo(false, true),
                            currentValues = isc.shallowClone(f.view.getCurrentValues()),
                            otherItem,
                            disabledFields, i;
                        OB.Utilities.fixNull250(currentValues);

                        // Applying read only logic.
                        if ((OB.Utilities.getValue(currentValues, 'documentStatus') !== 'DR' || ((OB.Utilities.getValue(currentValues, 'documentStatus') === 'DR') && (OB.Utilities.getValue(currentValues, 'saqbApprovetrx') === true)) ) && f.fields) {
                            var fields = f.fields;
                            for (var i = 0; i < fields.length; i++) {
                                f.disableItem(fields[i].name, true);
                            }
                        } else if (OB.Utilities.getValue(currentValues, 'documentStatus') !== 'DR' || ((OB.Utilities.getValue(currentValues, 'documentStatus') === 'DR') && (OB.Utilities.getValue(currentValues, 'saqbApprovetrx') === true))) {
                            var items = f.items;
                            for (var i = 0; i < items.length; i++) {
                                items[i].disabled = true;
                            }
                        } else if (((OB.Utilities.getValue(currentValues, 'documentStatus') === 'DR') && (OB.Utilities.getValue(currentValues, 'saqbApprovetrx') !== true)) && f.fields) {
                            var fields = f.fields;
                            for (var i = 0; i < fields.length; i++) {
                                if (fields[i].name === 'documentNo' || fields[i].name === 'orderDate' || fields[i].name === 'attentionHours') {
                                    continue;
                                }

                                if (fields[i].name === 'namePartner' && OB.Utilities.getValue(currentValues, 'cIFNif') === '9999999999') {
                                    continue;
                                }
                                if (fields[i].name === 'email' && OB.Utilities.getValue(currentValues, 'cIFNif') === '9999999999') {
                                    continue;
                                }

                                f.disableItem(fields[i].name, false);
                            }
                        } else if ((OB.Utilities.getValue(currentValues, 'documentStatus') === 'DR') && (OB.Utilities.getValue(currentValues, 'saqbApprovetrx') === true)) {
                            var items = f.items;
                            for (var i = 0; i < items.length; i++) {
                                items[i].disabled = false;
                            }
                        }
                        f.disableItem('attentionHours', true);
                        f.disableItem('orderDate', true);
                        f.disableItem('documentStatus', true);
                        f.disableItem('documentNo', true);
                        f.disableItem('summedLineAmount', true);
                        f.disableItem('grandTotalAmount', true);

                        // Applying read only logic.
                        f.disableItem('address1AliasRef', OB.Utilities.getValue(currentValues, 'cIFNif') === '9999999999');
                        f.disableItem('nEWAddress', OB.Utilities.getValue(currentValues, 'cIFNif') === '9999999999');
                        f.disableItem('deliveryInvoiceAddress', OB.Utilities.getValue(currentValues, 'cIFNif') === '9999999999');

                        if (OB.Utilities.getValue(currentValues, 'documentStatus') !== 'DR' || ((OB.Utilities.getValue(currentValues, 'documentStatus') === 'DR') && (OB.Utilities.getValue(currentValues, 'saqbApprovetrx') === true))) {
                            f.disableItem('namePartner', true);
                            f.disableItem('address1AliasRef', true);
                            f.disableItem('email', true);
                            f.disableItem('nEWAddress', true);
                            f.disableItem('deliveryInvoiceAddress', true);
                        }

                        // Applying display logic in grid.
                        if (!this.view.isShowingForm) {
                            f.disableItem('addressCompleteFac', (OB.Utilities.getValue(currentValues, 'deliveryInvoiceAddress') === false) === false);
                        }
                        // Applying display logic in grid.
                        if (!this.view.isShowingForm) {
                            f.disableItem('address1AliasRefFac', (OB.Utilities.getValue(currentValues, 'deliveryInvoiceAddress') === false) === false);
                        }
                        // Applying display logic in grid.
                        if (!this.view.isShowingForm) {
                            f.disableItem('phoneFac', (OB.Utilities.getValue(currentValues, 'deliveryInvoiceAddress') === false) === false);
                        }
                        // Applying display logic in grid.
                        /*if (!this.view.isShowingForm) {
                            f.disableItem('address1Fac', (OB.Utilities.getValue(currentValues, 'deliveryInvoiceAddress') === false) === false);
                        }*/
                        // Applying display logic in grid.
                        if (!this.view.isShowingForm) {
                            f.disableItem('address1AliasFac', (OB.Utilities.getValue(currentValues, 'deliveryInvoiceAddress') === false) === false);
                        }
                        /*     
            // Applying display logic in grid.
            
//            if (!this.view.isShowingForm) {
//              f.disableItem('address1Alias', (OB.Utilities.getValue(currentValues, 'nEWAddress') === true) === false);
//              if (OB.Utilities.getValue(currentValues, 'documentStatus') !== 'DR') {
//                f.disableItem('address1Alias', true);
//              }
//            }
            // Applying display logic in grid.
            if (!this.view.isShowingForm) {
              f.disableItem('billetValue', (OB.Utilities.getValue(currentValues, 'paymentMethod') === '498FE48D125D49129E3D1E691264F6A6') === false);
            }
            // Applying display logic in grid.
//            if (!this.view.isShowingForm) {
//              f.disableItem('bank', (OB.Utilities.getValue(currentValues, 'paymentMethod') === 'C69C5F7741914599939398DCD6D9895D') === false);
//            }
            // Applying display logic in grid.
            if (!this.view.isShowingForm) {
              f.disableItem('check', (OB.Utilities.getValue(currentValues, 'paymentMethod') === 'C69C5F7741914599939398DCD6D9895D') === false);
            }
            // Applying display logic in grid.
            if (!this.view.isShowingForm) {
              f.disableItem('card', (OB.Utilities.getValue(currentValues, 'paymentMethod') === 'E58C10F76F4748879ADFFFD862F7D3C0') === false);
            }
            // Applying display logic in grid.
            if (!this.view.isShowingForm) {
              f.disableItem('cardtype', (OB.Utilities.getValue(currentValues, 'paymentMethod') === 'E58C10F76F4748879ADFFFD862F7D3C0') === false);
            }
            // Applying display logic in grid.
            if (!this.view.isShowingForm) {
              f.disableItem('cardno', (OB.Utilities.getValue(currentValues, 'paymentMethod') === 'E58C10F76F4748879ADFFFD862F7D3C0') === false);
            }
            // Applying display logic in grid.
            if (!this.view.isShowingForm) {
              f.disableItem('expirationdate', (OB.Utilities.getValue(currentValues, 'paymentMethod') === 'E58C10F76F4748879ADFFFD862F7D3C0') === false);
            }
            // Applying display logic in grid.
            if (!this.view.isShowingForm) {
              f.disableItem('securitycode', (OB.Utilities.getValue(currentValues, 'paymentMethod') === 'E58C10F76F4748879ADFFFD862F7D3C0') === false);
            }
            // Applying display logic in grid.
            if (!this.view.isShowingForm) {
              f.disableItem('cardpropietary', (OB.Utilities.getValue(currentValues, 'paymentMethod') === 'E58C10F76F4748879ADFFFD862F7D3C0') === false);
            }
            // Applying display logic in grid.
            if (!this.view.isShowingForm) {
              f.disableItem('saqbFinancialEntity', (OB.Utilities.getValue(currentValues, 'paymentMethod') === 'E58C10F76F4748879ADFFFD862F7D3C0') === false);
            }
            // Applying display logic in grid.
            if (!this.view.isShowingForm) {
              f.disableItem('newcard', (OB.Utilities.getValue(currentValues, 'paymentMethod') === 'E58C10F76F4748879ADFFFD862F7D3C0') === false);
            }
            // disable forced in case the fields are set as read only per role
           */
                        // disable forced in case the fields are set as read only per role
                        disabledFields = form.view.disabledFields;
                        if (disabledFields) {
                            for (i = 0; i < disabledFields.length; i++) {
                                f.disableItem(disabledFields[i], true);
                            }
                        }
                    }
                }
            });
            this.viewGrid =
                isc.OBViewGrid.create({
                    view: this,
                    uiPattern: 'STD',
                    orderByClause: '-creationDate',
                    filterClause: true,
                    allowSummaryFunctions: true,
                    // List of properties that must be always included in this grid
                    requiredGridProperties: [
                        'id',
                        'client',
                        'organization',
                        'updatedBy',
                        'updated',
                        'creationDate',
                        'createdBy',
                        'orderDate',
                        'process',
                        'documentStatus',
                        'deliveryInvoiceAddress',
                        'organization',
                        'client'
                    ],
                    // the this is the view instance
                    fields: this.gridFields
                })

            ;
            this.Super('initWidget', arguments);


        },

    }
});
