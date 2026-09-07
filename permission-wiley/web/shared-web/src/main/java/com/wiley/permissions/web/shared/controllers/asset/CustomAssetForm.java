package com.wiley.permissions.web.shared.controllers.asset;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.AutoPopulatingList;

import com.wiley.permissions.domain.persistence.permissions.AssetFile;
import com.wiley.permissions.domain.persistence.permissions.AssetUseFile;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.domain.persistence.permissions.Currency;
import com.wiley.permissions.domain.persistence.permissions.ExtendedAssetUse;
import com.wiley.permissions.domain.persistence.permissions.PagePosition;
import com.wiley.permissions.domain.persistence.permissions.PurchaseOrder;
import com.wiley.permissions.domain.persistence.permissions.Size;
import com.wiley.permissions.domain.persistence.permissions.Usage;

public class CustomAssetForm {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CustomAssetForm.class);

	private ExtendedAssetUse extendedAssetUse = new ExtendedAssetUse();
	private PurchaseOrder po = new PurchaseOrder();
	private int usageCount;
	private boolean createPO;
	private boolean createContract;
	private boolean finish;
	private boolean addAnother;
	private AutoPopulatingList<UsageForm> usages = new AutoPopulatingList<UsageForm> (UsageForm.class);
	private List<AssetUseFile> files = new ArrayList<AssetUseFile> ();
	private boolean makeItDefault;
	private AssetFile thumbnail;
	private boolean inEditMode;

	public ExtendedAssetUse getExtendedAssetUse() {
		return extendedAssetUse;
	}

	public void setExtendedAssetUse(ExtendedAssetUse extendedAssetUse) {
		this.extendedAssetUse = extendedAssetUse;
	}

	public Boolean getInEditMode() {
		return inEditMode;
	}

	public void setInEditMode(Boolean inEditMode) {
		this.inEditMode = inEditMode;
	}

	public AssetFile getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(AssetFile thumbnail) {
		this.thumbnail = thumbnail;
	}

	public PurchaseOrder getPo() {
		return po;
	}

	public void setPo(PurchaseOrder po) {
		this.po = po;
	}

	public int getUsageCount() {
		return usageCount;
	}

	public void setUsageCount(int usageCount) {
		this.usageCount = usageCount;
	}

	public boolean isCreatePO() {
		return createPO;
	}

	public void setCreatePO(boolean createPO) {
		this.createPO = createPO;
	}

	public boolean isFinish() {
		return finish;
	}

	public void setFinish(boolean finish) {
		this.finish = finish;
	}

	public boolean isAddAnother() {
		return addAnother;
	}

	public void setAddAnother(boolean addAnother) {
		this.addAnother = addAnother;
	}

	public boolean isCreateContract() {
		return createContract;
	}

	public void setCreateContract(boolean createContract) {
		this.createContract = createContract;
	}

	public boolean isMakeItDefault() {
		return makeItDefault;
	}

	public void setMakeItDefault(boolean makeItDefault) {
		this.makeItDefault = makeItDefault;
	}

	public List<AssetUseFile> getFiles() {
		return files;
	}

	public void setFiles(List<AssetUseFile> files) {
		this.files = files;
	}

	public List<UsageForm> getUsages() {
		return usages;
	}

	public void setUsages(AutoPopulatingList<UsageForm> usages) {
		this.usages = usages;
	}

	public void addUsageForm (UsageForm usageForm) {
		usages.add(usageForm);
		usageCount++;
	}

	public static class UsageForm {
		private Usage usage;
		private Size size;
		private Component component;
		private String position;
		private String finalPage;
		private PagePosition pagePosition;
		private double estimatedCost = 0;
		private Currency estimatedCurrency;

		public UsageForm() {
			this.usage = Usage.FRONT_COVER;  // default option
		}

		public UsageForm(Usage usage, Size size, Component component, String position, String finalPage, PagePosition pagePosition, 
				double estimatedCost, Currency estimatedCurrency) {
			if (usage == null) usage = Usage.FRONT_COVER;  // default option
			this.usage = usage;
			this.size = size;
			this.component = component;
			this.position = position;
			this.finalPage = finalPage;
			this.pagePosition = pagePosition;
			this.estimatedCost = estimatedCost;
			this.estimatedCurrency = estimatedCurrency;
		}

		public Usage getUsage() {
			return usage;
		}

		public void setUsage(Usage usage) {
			this.usage = usage;
		}

		public Size getSize() {
			return size;
		}

		public void setSize(Size size) {
			this.size = size;
		}

		public double getEstimatedCost() {
			return estimatedCost;
		}

		public void setEstimatedCost(double estimatedCost) {
			this.estimatedCost = estimatedCost;
		}

		public Component getComponent() {
			return component;
		}

		public void setComponent(Component component) {
			this.component = component;
		}

		public String getPosition() {
			return position;
		}

		public void setPosition(String position) {
			this.position = position;
		}

		public String getFinalPage() {
			return finalPage;
		}

		public void setFinalPage(String finalPage) {
			this.finalPage = finalPage;
		}

		public PagePosition getPagePosition() {
			return pagePosition;
		}

		public void setPagePosition(PagePosition pagePosition) {
			this.pagePosition = pagePosition;
		}

		public Currency getEstimatedCurrency() {
			return estimatedCurrency;
		}

		public void setEstimatedCurrency(Currency estimatedCurrency) {
			this.estimatedCurrency = estimatedCurrency;
		}

		@Override
		public String toString() {
			return "UsageForm [usage=" + (null != usage ? usage.getCode() : null) + 
					", size=" + (null != size ? size.getCode() : null) + "]";
		}
	}
}
