/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

// Set of unit tests to check combos, execution depends on module org.openbravo.retail.postest
// Configurations can be found at https://docs.google.com/a/openbravo.com/spreadsheet/ccc?key=0AmPGxKaZaJn-dEkyb21kR2lvOHBvM04zUXBpV1VyMlE&usp=drive_web#gid=1
var t = new OB.POS.Test([
// Set BDC mode                         
{
  actions: [function (callback) {
    window.originalBDCMode = OB.POS.modelterminal.get('terminal').bestDealCase;
    OB.POS.modelterminal.get('terminal').bestDealCase = true;
    callback();
  }]
},

// Basic combo configuration
{
  actions: [{
    func: 'addProduct',
    p: ['A', 'C']
  }],
  assert: [{
    func: 'checkGross',
    result: 12.50
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'C', 'A']
  }],
  assert: [{
    func: 'checkGross',
    result: 32.50
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'C', 'A', 'C']
  }],
  assert: [{
    func: 'checkGross',
    result: 25.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'C', 'A', 'C', 'C']
  }],
  assert: [{
    func: 'checkGross',
    result: 40.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'B', 'C']
  }],
  assert: [{
    func: 'checkGross',
    result: 22.50
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['B', 'C', 'A']
  }],
  assert: [{
    func: 'checkGross',
    result: 22.50
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'B', 'C', 'D']
  }],
  assert: [{
    func: 'checkGross',
    result: 48.50
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'C', 'B', 'E']
  }],
  assert: [{
    func: 'checkGross',
    result: 37.50
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'B', 'C', 'C']
  }],
  assert: [{
    func: 'checkGross',
    result: 25.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'C', 'B', 'C']
  }],
  assert: [{
    func: 'checkGross',
    result: 25.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['B', 'B', 'C', 'D']
  }],
  assert: [{
    func: 'checkGross',
    result: 48.50
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['B', 'D', 'B', 'C']
  }],
  assert: [{
    func: 'checkGross',
    result: 48.50
  }]
},

// More than 1 unit per family
{
  actions: [{
    func: 'addProduct',
    p: ['F', 'F']
  }],
  assert: [{
    func: 'checkGross',
    result: 36.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['F', 'F', 'F']
  }],
  assert: [{
    func: 'checkGross',
    result: 56.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['F', 'G']
  }],
  assert: [{
    func: 'checkGross',
    result: 45.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['F', 'F', 'G', 'G']
  }],
  assert: [{
    func: 'checkGross',
    result: 90.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['F', 'G', 'F', 'G']
  }],
  assert: [{
    func: 'checkGross',
    result: 90.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['F', 'G', 'G']
  }],
  assert: [{
    func: 'checkGross',
    result: 74.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['F', 'G', 'G', 'G']
  }],
  assert: [{
    func: 'checkGross',
    result: 99.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['F', 'G', 'F']
  }],
  assert: [{
    func: 'checkGross',
    result: 65.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['H', 'H', 'J']
  }],
  assert: [{
    func: 'checkGross',
    result: 35.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['H', 'H', 'I', 'I', 'J', 'J']
  }],
  assert: [{
    func: 'checkGross',
    result: 132.00
  }, {
    func: 'checkBestDealCase',
    result: 85.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['H', 'I', 'J']
  }],
  assert: [{
    func: 'checkGross',
    result: 83.50
  }, {
    func: 'checkBestDealCase',
    result: 42.50
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['H', 'I', 'J', 'J']
  }],
  assert: [{
    func: 'checkGross',
    result: 123.50
  }, {
    func: 'checkBestDealCase',
    result: 82.50
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['H', 'I', 'J', 'I']
  }],
  assert: [{
    func: 'checkGross',
    result: 65.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['H', 'I', 'J', 'H']
  }],
  assert: [{
    func: 'checkGross',
    result: 63.50
  }, {
    func: 'checkBestDealCase',
    result: 57.50
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['H', 'J']
  }],
  assert: [{
    func: 'checkGross',
    result: 55.00
  }]
},

// delete lines: merge and rearrange combos
{
  actions: [{
    func: 'addProduct',
    p: ['A', 'C', 'A']
  }, {
    func: 'deleteLine',
    p: {
      line: 1
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 40
  }, {
    func: 'checkNumOfLines',
    result: 1
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'B', 'C']
  }, {
    func: 'deleteLine',
    p: {
      line: 0
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 12.50
  }, {
    func: 'checkNumOfLines',
    result: 2
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['B', 'C', 'D']
  }, {
    func: 'deleteLine',
    p: {
      line: 1
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 36.00
  }, {
    func: 'checkNumOfLines',
    result: 2
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'A', 'C']
  }, {
    func: 'deleteLine',
    p: {
      line: 0
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 12.50
  }, {
    func: 'checkNumOfLines',
    result: 2
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'A', 'C']
  }, {
    func: 'deleteLine',
    p: {
      line: 2
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 12.50
  }, {
    func: 'checkNumOfLines',
    result: 2
  }]
},

{
  actions: [{
    func: 'addProduct',
    p: ['H', 'I', 'J']
  }, {
    func: 'deleteLine',
    p: {
      line: 0
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 68.50
  }, {
    func: 'checkBestDealCase',
    result: 67.60
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['H', 'I', 'J']
  }, {
    func: 'deleteLine',
    p: {
      line: 1
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 55
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['H', 'I', 'J']
  }, {
    func: 'deleteLine',
    p: {
      line: 2
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 43.5
  }, {
    func: 'checkBestDealCase',
    result: 42.60
  }]
},

// discount (no promotions) and merge
{
  actions: [{
    func: 'addProduct',
    p: ['A', 'A', 'C']
  }, {
    func: 'setLinePrice',
    p: {
      line: 2,
      price: 1
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 13.5
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'A', 'C']
  }, {
    func: 'setLinePrice',
    p: {
      line: 2,
      price: 1
    }
  }, {
    func: 'deleteLine',
    p: {
      line: 1
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 21
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['B', 'D', 'B']
  }, {
    func: 'setLinePrice',
    p: {
      line: 2,
      price: 8
    }
  }, {
    func: 'addProduct',
    p: ['D']
  }],
  assert: [{
    func: 'checkGross',
    result: 70.20
  }]
},

// best deal case
{
  actions: [{
    func: 'addProduct',
    p: ['G']
  }],
  assert: [{
    func: 'checkGross',
    result: 28.50
  }, {
    func: 'checkBestDealCase',
    result: 27.60
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['G', 'G']
  }],
  assert: [{
    func: 'checkGross',
    result: 54.00
  }, {
    func: 'checkBestDealCase',
    result: 54.00
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['G', 'G', 'G']
  }],
  assert: [{
    func: 'checkGross',
    result: 82.50
  }, {
    func: 'checkBestDealCase',
    result: 81.60
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['G', 'G', 'F']
  }],
  assert: [{
    func: 'checkGross',
    result: 74.00
  }, {
    func: 'checkBestDealCase',
    result: 72.60
  }]
},

// best deal case with discretionary discounts
{
  actions: [{
    func: 'addProduct',
    p: ['A', 'C']
  }, {
    func: 'addDiscretionaryDiscount',
    p: {
      disc: {
        name: 'disc_5_ var_perc',
        amt: 5
      },
      line: 0
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 34
  }, {
    func: 'checkBestDealCase',
    result: 34
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'C']
  }, {
    func: 'addDiscretionaryDiscount',
    p: {
      disc: {
        name: 'disc_5_ var_perc',
        amt: 5
      },
      line: 1
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 34.25
  }, {
    func: 'checkBestDealCase',
    result: 34.25
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'C', 'C']
  }, {
    func: 'addDiscretionaryDiscount',
    p: {
      disc: {
        name: 'disc_5_ var_perc',
        amt: 5
      },
      line: 2
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 26.75
  }, {
    func: 'checkBestDealCase',
    result: 26.75
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['A', 'C', 'C']
  }, {
    func: 'addDiscretionaryDiscount',
    p: {
      disc: {
        name: 'disc_5_ var_perc',
        amt: 5
      },
      line: 2
    }
  }, {
    func: 'deleteLine',
    p: {
      line: 0
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 30
  }, {
    func: 'checkBestDealCase',
    result: 30
  }]
},

// Independent subgroups
{
  actions: [{
    func: 'addProduct',
    p: ['G', 'I']
  }],
  assert: [{
    func: 'checkGross',
    result: 57.00
  }, {
    func: 'checkBestDealCase',
    result: 55.20
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['F', 'F', 'G', 'G', 'G']
  }],
  assert: [{
    func: 'checkGross',
    result: 119.00
  }, {
    func: 'checkBestDealCase',
    result: 117.60
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['I', 'I', 'I', 'J']
  }],
  assert: [{
    func: 'checkGross',
    result: 125.50
  }, {
    func: 'checkBestDealCase',
    result: 77.60
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['F', 'F', 'G', 'G', 'G', 'I', 'I', 'I', 'J']
  }],
  assert: [{
    func: 'checkGross',
    result: 244.50
  }, {
    func: 'checkBestDealCase',
    result: 195.20
  }]
},

// By total Discounts in BDC mode
{
  actions: [{
    func: 'addProduct',
    p: ['K', 'L']
  }],
  assert: [{
    func: 'checkGross',
    result: 362
  }, {
    func: 'checkBestDealCase',
    result: 320
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['K', 'L', 'L']
  }],
  assert: [{
    func: 'checkGross',
    result: 454
  }, {
    func: 'checkBestDealCase',
    result: 400
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['K', 'L', 'L', 'M']
  }],
  assert: [{
    func: 'checkGross',
    result: 463
  }, {
    func: 'checkBestDealCase',
    result: 400
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['K', 'L', 'L', 'M', 'M']
  }],
  assert: [{
    func: 'checkGross',
    result: 472
  }, {
    func: 'checkBestDealCase',
    result: 400
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['K', 'L', 'L', 'M', 'M', 'M']
  }],
  assert: [{
    func: 'checkGross',
    result: 481
  }, {
    func: 'checkBestDealCase',
    result: 410
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['K', 'K']
  }],
  assert: [{
    func: 'checkGross',
    result: 540
  }, {
    func: 'checkBestDealCase',
    result: 300
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['K', 'K', 'K']
  }],
  assert: [{
    func: 'checkGross',
    result: 810
  }, {
    func: 'checkBestDealCase',
    result: 600
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['K', 'K', 'K', 'K', 'K', 'K']
  }],
  assert: [{
    func: 'checkGross',
    result: 1620
  }, {
    func: 'checkBestDealCase',
    result: 1440
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['K', 'L', 'L', 'L', 'M']
  }],
  assert: [{
    func: 'checkGross',
    result: 555
  }, {
    func: 'checkBestDealCase',
    result: 310
  }, {
    func: 'checkPromotions',
    result: [147.54, 147.54, 4.92]
  }]
},

// Standard mode (no BDC)
{
  actions: [function (callback) {
    OB.POS.modelterminal.get('terminal').bestDealCase = false;
    callback();
  }]
},

// By total Discounts Standard mode 
{
  actions: [{
    func: 'addProduct',
    p: ['K', 'L']
  }],
  assert: [{
    func: 'checkGross',
    result: 289.6
  }, {
    func: 'checkPromotions',
    result: [84, 26.4]
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['M']
  }, {
    func: 'setQty',
    p: {
      qty: 39,
      line: 0
    }
  }],
  assert: [{
    func: 'checkGross',
    result: 351
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['K', 'L', 'M']
  }],
  assert: [{
    func: 'checkGross',
    result: 298.6
  }, {
    func: 'checkPromotions',
    result: [84, 26.4, 1]
  }]
}, {
  actions: [{
    func: 'addProduct',
    p: ['K', 'L', 'L']
  }],
  assert: [{
    func: 'checkGross',
    result: 363.2
  }, {
    func: 'checkPromotions',
    result: [84, 52.8]
  }]
},

// Reset original BDC mode   
{
  actions: [function (callback) {
    OB.POS.modelterminal.get('terminal').bestDealCase = window.originalBDCMode;
    callback();
  }]
}]);
t.execute();