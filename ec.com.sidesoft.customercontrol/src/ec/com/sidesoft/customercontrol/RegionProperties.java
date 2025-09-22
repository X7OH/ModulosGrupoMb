package ec.com.sidesoft.customercontrol;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.mobile.core.model.ModelExtension;

@Qualifier(Region.regionPropertyExtension)
public class RegionProperties extends ModelExtension {
	@Override
	public List<HQLProperty> getHQLProperties(Object params) {
		ArrayList<HQLProperty> list = new ArrayList<HQLProperty>() {
			{
				add(new HQLProperty("reg.id", "id"));
				add(new HQLProperty("reg.name", "name"));
				add(new HQLProperty("reg.name", "_identifier"));
			}
		};
		return list;
	}
}
