package cl.votainteligente.inspector.client.presenters;

import cl.votainteligente.inspector.client.i18n.ApplicationMessages;
import cl.votainteligente.inspector.client.services.BillServiceAsync;
import cl.votainteligente.inspector.client.services.CategoryServiceAsync;
import cl.votainteligente.inspector.client.services.ParlamentarianServiceAsync;
import cl.votainteligente.inspector.client.uihandlers.HomeUiHandlers;
import cl.votainteligente.inspector.model.Bill;
import cl.votainteligente.inspector.model.Category;
import cl.votainteligente.inspector.model.Parlamentarian;

import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyStandard;
import com.gwtplatform.mvp.client.proxy.*;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ImageCell;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.view.client.*;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HomePresenter extends Presenter<HomePresenter.MyView, HomePresenter.MyProxy> implements HomeUiHandlers {
	public static final String PLACE = "home";

	public interface MyView extends View, HasUiHandlers<HomeUiHandlers> {
		String getParlamentarianSearch();
		void setParlamentarianSearch(String parlamentarianSearch);
		String getCategorySearch();
		void setCategorySearch(String categorySearch);
		CellTable<Parlamentarian> getParlamentarianTable();
		CellTable<Category> getCategoryTable();
		CellTable<Bill> getBillTable();
		void setParlamentarianDisplay(String parlamentarianName);
		void setCategoryDisplay(String categoryName);
		void setParlamentarianImage(String parlamentarianImage);
		void setSelectedType(SelectionType selectedType);
		void setParlamentarianMessage(String message);
		void hideParlamentarianMessage();
		void setCategoryMessage(String message);
		void hideCategoryMessage();
		void setBillMessage(String message);
		void hideBillMessage();
		void showBillTable();
		void hideBillTable();
		void notificationSelectCategory();
		void notificationSelectParliamentarian();
		void notificationSelectHidden();
		void displaySelectionNone();
		void displaySelectionParliamentarian();
		void displaySelectionCategory();
	}

	public enum SelectionType {
		SELECTED_PARLAMENTARIAN,
		SELECTED_CATEGORY
	}

	@ProxyStandard
	@NameToken(PLACE)
	public interface MyProxy extends ProxyPlace<HomePresenter> {
	}

	@Inject
	private ApplicationMessages applicationMessages;
	@Inject
	private PlaceManager placeManager;
	@Inject
	private ParlamentarianServiceAsync parlamentarianService;
	@Inject
	private CategoryServiceAsync categoryService;
	@Inject
	private BillServiceAsync billService;

	private AbstractDataProvider<Parlamentarian> parlamentarianData;
	private AbstractDataProvider<Category> categoryData;
	private AbstractDataProvider<Bill> billData;
	private Parlamentarian selectedParlamentarian;
	private Category selectedCategory;
	private SelectionType selectedType;

	@Inject
	public HomePresenter(EventBus eventBus, MyView view, MyProxy proxy) {
		super(eventBus, view, proxy);
		getView().setUiHandlers(this);
	}

	@Override
	protected void onReset() {
		parlamentarianData = new ListDataProvider<Parlamentarian>();
		categoryData = new ListDataProvider<Category>();
		billData = new ListDataProvider<Bill>();
		resetSelection();
		setupSelection(SelectionType.SELECTED_PARLAMENTARIAN);
		initParlamentarianTable();
		initCategoryTable();
		initBillTable();
		initDataLoad();
		getView().hideBillTable();
		getView().displaySelectionNone();
	}

	@Override
	protected void revealInParent() {
		fireEvent(new RevealContentEvent(MainPresenter.SLOT_MAIN_CONTENT, this));
	}

	public void initDataLoad() {
		parlamentarianService.getAllParlamentarians(new AsyncCallback<List<Parlamentarian>>() {

			@Override
			public void onFailure(Throwable caught) {
				Window.alert(applicationMessages.getErrorParlamentarianList());
			}

			@Override
			public void onSuccess(List<Parlamentarian> result) {
				if (result != null) {
					ListDataProvider<Parlamentarian> data = new ListDataProvider<Parlamentarian>(result);
					setParlamentarianData(data);
				}
			}
		});

		categoryService.getAllCategories(new AsyncCallback<List<Category>>() {

			@Override
			public void onFailure(Throwable caught) {
				Window.alert(applicationMessages.getErrorCategoryList());
			}

			@Override
			public void onSuccess(List<Category> result) {
				if (result != null) {
					ListDataProvider<Category> data = new ListDataProvider<Category>(result);
					setCategoryData(data);
				}
			}
		});
		setBillTable();
		getView().hideParlamentarianMessage();
		getView().hideCategoryMessage();
		getView().hideBillMessage();
		getView().hideBillTable();
		getView().notificationSelectHidden();
		getView().displaySelectionNone();
	}

	@Override
	public void searchParlamentarian(String keyWord) {
		getView().hideParlamentarianMessage();
		getView().setParlamentarianDisplay(applicationMessages.getGeneralParlamentarian());
		getView().setParlamentarianImage("images/parlamentarian/large/avatar.png");
		if (selectedParlamentarian != null) {
			getView().getParlamentarianTable().getSelectionModel().setSelected(selectedParlamentarian, false);
			selectedParlamentarian = null;
		}

		if (keyWord == null || keyWord.length() == 0 || keyWord.equals("")) {
			if (selectedType.equals(SelectionType.SELECTED_PARLAMENTARIAN)) {
				initDataLoad();
			} else if (selectedType.equals(SelectionType.SELECTED_CATEGORY)) {
				List<Category> categories = new ArrayList<Category>();
				categories.add(selectedCategory);
				searchParlamentarian(categories);
			}
		} else {
			parlamentarianService.searchParlamentarian(keyWord, new AsyncCallback<List<Parlamentarian>>() {

				@Override
				public void onFailure(Throwable caught) {
					Window.alert(applicationMessages.getErrorParlamentarianSearch());
				}

				@Override
				public void onSuccess(List<Parlamentarian> result) {
					final List<Parlamentarian> keywordSearchParlamentarianList = result;

					if (result != null) {
						if (selectedType.equals(SelectionType.SELECTED_PARLAMENTARIAN)) {
							ListDataProvider<Parlamentarian> data = new ListDataProvider<Parlamentarian>(result);
							setParlamentarianData(data);
						} else if (selectedType.equals(SelectionType.SELECTED_CATEGORY)) {
							List<Category> categories = new ArrayList<Category>();
							categories.add(selectedCategory);

							parlamentarianService.searchParlamentarian(categories, new AsyncCallback<List<Parlamentarian>>() {

								@Override
								public void onFailure(Throwable caught) {
									Window.alert(applicationMessages.getErrorParlamentarianList());
								}

								@Override
								public void onSuccess(List<Parlamentarian> result) {
									if (result != null) {
										List<Parlamentarian> resultList = new ArrayList<Parlamentarian>();

										for (Parlamentarian parlamentarian : keywordSearchParlamentarianList) {
											if (result.contains(parlamentarian)) {
												resultList.add(parlamentarian);
											}
										}

										ListDataProvider<Parlamentarian> data = new ListDataProvider<Parlamentarian>(resultList);
										setParlamentarianData(data);
									}
								}
							});
						}

						if (!selectedType.equals(SelectionType.SELECTED_CATEGORY)) {
							searchCategory(result);
						}
						if (result.size() == 0) {
							getView().setParlamentarianMessage(applicationMessages.getGeneralNoMatches());
						}
					}
				}
			});
		}
	}

	@Override
	public void searchParlamentarian(List<Category> categories) {
		getView().hideParlamentarianMessage();
		getView().setParlamentarianDisplay(applicationMessages.getGeneralParlamentarian());
		getView().setParlamentarianImage("images/parlamentarian/large/avatar.png");
		if (selectedParlamentarian != null) {
			getView().getParlamentarianTable().getSelectionModel().setSelected(selectedParlamentarian, false);
			selectedParlamentarian = null;
		}

		parlamentarianService.searchParlamentarian(categories, new AsyncCallback<List<Parlamentarian>>() {

			@Override
			public void onFailure(Throwable caught) {
				Window.alert(applicationMessages.getErrorParlamentarianCategorySearch());
			}

			@Override
			public void onSuccess(List<Parlamentarian> result) {
				if (result != null) {
					ListDataProvider<Parlamentarian> data = new ListDataProvider<Parlamentarian>(result);
					setParlamentarianData(data);
				}
			}
		});
	}

	@Override
	public void searchCategory(String keyWord) {
		getView().hideCategoryMessage();
		getView().setCategoryDisplay(applicationMessages.getGeneralCategory());
		if (selectedCategory != null) {
			getView().getCategoryTable().getSelectionModel().setSelected(selectedCategory, false);
			selectedCategory = null;
		}

		if (keyWord == null || keyWord.length() == 0 || keyWord.equals("")) {
			if (selectedType.equals(SelectionType.SELECTED_CATEGORY)) {
				initDataLoad();
			} else if (selectedType.equals(SelectionType.SELECTED_PARLAMENTARIAN)) {
				List<Parlamentarian> parlamentarians = new ArrayList<Parlamentarian>();
				parlamentarians.add(selectedParlamentarian);
				searchCategory(parlamentarians);
			}
		} else {
			categoryService.searchCategory(keyWord, new AsyncCallback<List<Category>>() {

				@Override
				public void onFailure(Throwable caught) {
					Window.alert(applicationMessages.getErrorCategoryList());
				}

				@Override
				public void onSuccess(List<Category> result) {
					if (result != null) {
						if (selectedType.equals(SelectionType.SELECTED_CATEGORY)) {
							ListDataProvider<Category> data = new ListDataProvider<Category>(result);
							setCategoryData(data);
						} else if (selectedType.equals(SelectionType.SELECTED_PARLAMENTARIAN)) {
							final List<Category> keywordSearchCategoryList = result;

							List<Parlamentarian> parlamentarians = new ArrayList<Parlamentarian>();
							parlamentarians.add(selectedParlamentarian);

							categoryService.searchCategory(parlamentarians, new AsyncCallback<List<Category>>() {

								@Override
								public void onFailure(Throwable caught) {
									Window.alert(applicationMessages.getErrorCategoryParlamentarianSearch());
								}

								@Override
								public void onSuccess(List<Category> result) {
									if (result != null) {
										List<Category> resultList = new ArrayList<Category>();

										for (Category category : keywordSearchCategoryList) {
											if (result.contains(category)) {
												resultList.add(category);
											}
										}

										ListDataProvider<Category> data = new ListDataProvider<Category>(resultList);
										setCategoryData(data);
									}
								}
							});
						}

						if (!selectedType.equals(SelectionType.SELECTED_PARLAMENTARIAN)) {
							searchParlamentarian(result);
						}
						if (result.size() == 0) {
							getView().setCategoryMessage(applicationMessages.getGeneralNoMatches());
						}
					}
				}
			});
		}
	}

	@Override
	public void searchCategory(List<Parlamentarian> parlamentarians) {
		getView().hideCategoryMessage();
		getView().setCategoryDisplay(applicationMessages.getGeneralCategory());
		if (selectedCategory != null) {
			getView().getCategoryTable().getSelectionModel().setSelected(selectedCategory, false);
			selectedCategory = null;
		}

		categoryService.searchCategory(parlamentarians, new AsyncCallback<List<Category>>() {

			@Override
			public void onFailure(Throwable caught) {
				Window.alert(applicationMessages.getErrorCategoryParlamentarianSearch());
			}

			@Override
			public void onSuccess(List<Category> result) {
				if (result != null) {
					ListDataProvider<Category> data = new ListDataProvider<Category>(result);
					setCategoryData(data);
				}
			}
		});
	}

	@Override
	public void searchBill(Long parlamentarianId, Long categoryId) {
		getView().hideBillMessage();
		billService.searchBills(parlamentarianId, categoryId, new AsyncCallback<List<Bill>>() {

			@Override
			public void onFailure(Throwable caught) {
				Window.alert(applicationMessages.getErrorBillList());
			}

			@Override
			public void onSuccess(List<Bill> result) {
				if (result != null) {
					ListDataProvider<Bill> data = new ListDataProvider<Bill>(result);
					setBillData(data);
					if (result.size() == 0) {
						getView().setBillMessage(applicationMessages.getGeneralNoMatches());
					}
				}
			}
		});
	}

	public AbstractDataProvider<Parlamentarian> getParlamentarianData() {
		return parlamentarianData;
	}

	public void setParlamentarianData(AbstractDataProvider<Parlamentarian> data) {
		parlamentarianData = data;
		parlamentarianData.addDataDisplay(getView().getParlamentarianTable());
	}

	public AbstractDataProvider<Category> getCategoryData() {
		return categoryData;
	}

	public void setCategoryData(AbstractDataProvider<Category> data) {
		categoryData = data;
		categoryData.addDataDisplay(getView().getCategoryTable());
	}

	public AbstractDataProvider<Bill> getBillData() {
		return billData;
	}

	public void setBillData(AbstractDataProvider<Bill> data) {
		billData = data;
		billData.addDataDisplay(getView().getBillTable());
	}

	public void initParlamentarianTable() {
		while (getView().getParlamentarianTable().getColumnCount() > 0) {
			getView().getParlamentarianTable().removeColumn(0);
		}

		// Creates image column
		Column<Parlamentarian, String> imageColumn = new Column<Parlamentarian, String>(new ImageCell()){
			@Override
			public String getValue(Parlamentarian parlamentarian) {
				if (parlamentarian.getImage() != null) {
					return "images/parlamentarian/small/" + parlamentarian.getImage();
				} else {
					return "images/parlamentarian/small/avatar.png";
				}
			}
		};

		// Adds name column to table
		getView().getParlamentarianTable().addColumn(imageColumn, "");

		// Creates name column
		TextColumn<Parlamentarian> nameColumn = new TextColumn<Parlamentarian>() {
			@Override
			public String getValue(Parlamentarian parlamentarian) {
				return parlamentarian.toString();
			}
		};

		// Sets sortable name column
		nameColumn.setSortable(true);
		ListHandler<Parlamentarian> nameSortHandler = new ListHandler<Parlamentarian>(((ListDataProvider<Parlamentarian>) parlamentarianData).getList());
		getView().getParlamentarianTable().addColumnSortHandler(nameSortHandler);
		nameSortHandler.setComparator(nameColumn, new Comparator<Parlamentarian>() {
			public int compare(Parlamentarian o1, Parlamentarian o2) {
				return o1.getLastName().compareTo(o2.getLastName());
			}
		});

		// Adds name column to table
		getView().getParlamentarianTable().addColumn(nameColumn, applicationMessages.getGeneralParlamentarian());

		// Creates party column
		TextColumn<Parlamentarian> partyColumn = new TextColumn<Parlamentarian>() {
			@Override
			public String getValue(Parlamentarian parlamentarian) {
				return parlamentarian.getParty().getName();
			}
		};

		// Sets sortable party column
		partyColumn.setSortable(true);
		ListHandler<Parlamentarian> partySortHandler = new ListHandler<Parlamentarian>(((ListDataProvider<Parlamentarian>) parlamentarianData).getList());
		getView().getParlamentarianTable().addColumnSortHandler(partySortHandler);
		partySortHandler.setComparator(nameColumn, new Comparator<Parlamentarian>() {
			public int compare(Parlamentarian o1, Parlamentarian o2) {
				return o1.getParty().getName().compareTo(o2.getParty().getName());
			}
		});

		// Adds party column to table
		getView().getParlamentarianTable().addColumn(partyColumn, applicationMessages.getGeneralParty());

		// Creates action profile column
		Column<Parlamentarian, Parlamentarian> profileColumn = new Column<Parlamentarian, Parlamentarian>(new ActionCell<Parlamentarian>("", new ActionCell.Delegate<Parlamentarian>() {

			@Override
			public void execute(Parlamentarian parlamentarian) {
				PlaceRequest placeRequest = new PlaceRequest(ParlamentarianPresenter.PLACE);
				placeManager.revealPlace(placeRequest.with(ParlamentarianPresenter.PARAM_PARLAMENTARIAN_ID, parlamentarian.getId().toString()));
			}
		}) {
			@Override
			public void render(Cell.Context context, Parlamentarian value, SafeHtmlBuilder sb) {
				sb.append(new SafeHtml() {

					@Override
					public String asString() {
						return "<div class=\"profileButton\"></div>";
					}
				});
			}
		}) {

			@Override
			public Parlamentarian getValue(Parlamentarian parlamentarian) {
				return parlamentarian;
			}
		};

		// Adds action profile column to table
		getView().getParlamentarianTable().addColumn(profileColumn, applicationMessages.getGeneralProfile());

		// Sets selection model for each row
		final SingleSelectionModel<Parlamentarian> selectionModel = new SingleSelectionModel<Parlamentarian>(Parlamentarian.KEY_PROVIDER);
		getView().getParlamentarianTable().setSelectionModel(selectionModel);
		selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
			public void onSelectionChange(SelectionChangeEvent event) {

				if (selectionModel.getSelectedObject() != null) {
					if (selectedType.equals(SelectionType.SELECTED_PARLAMENTARIAN)) {
						List<Parlamentarian> parlamentarians = new ArrayList<Parlamentarian>();
						parlamentarians.add(selectionModel.getSelectedObject());
						searchCategory(parlamentarians);
						getView().notificationSelectParliamentarian();
						getView().displaySelectionParliamentarian();
					}

					selectedParlamentarian = selectionModel.getSelectedObject();
					getView().setParlamentarianDisplay(selectedParlamentarian.toString());
					if (selectedParlamentarian.getImage() == null) {
						getView().setParlamentarianImage("images/parlamentarian/large/avatar.png");
					} else {
						getView().setParlamentarianImage("images/parlamentarian/large/" + selectedParlamentarian.getImage());
					}
					setBillTable();
				}
			}
		});
	}

	public void initCategoryTable() {
		while (getView().getCategoryTable().getColumnCount() > 0) {
			getView().getCategoryTable().removeColumn(0);
		}

		// Creates name column
		TextColumn<Category> nameColumn = new TextColumn<Category>() {
			@Override
			public String getValue(Category category) {
				return category.getName();
			}
		};

		// Sets sortable name column
		nameColumn.setSortable(true);
		ListHandler<Category> sortHandler = new ListHandler<Category>(((ListDataProvider<Category>) categoryData).getList());
		getView().getCategoryTable().addColumnSortHandler(sortHandler);
		sortHandler.setComparator(nameColumn, new Comparator<Category>() {
			public int compare(Category o1, Category o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		// Adds name column to table
		getView().getCategoryTable().addColumn(nameColumn, applicationMessages.getGeneralCategory());

		// Creates action suscription column
		Column<Category, Category> suscriptionColumn = new Column<Category, Category>(new ActionCell<Category>("", new ActionCell.Delegate<Category>() {

			@Override
			public void execute(Category category) {
				// TODO: add category suscription servlet
				PlaceRequest placeRequest = new PlaceRequest(SubscriptionPresenter.PLACE);
				placeManager.revealPlace(placeRequest.with(SubscriptionPresenter.PARAM_CATEGORY_ID, category.getId().toString()));
			}
		}) {
			@Override
			public void render(Cell.Context context, Category value, SafeHtmlBuilder sb) {
				sb.append(new SafeHtml() {

					@Override
					public String asString() {
						return "<div class=\"suscribeButtonCategory\"></div>";
					}
				});
			}
		}) {

			@Override
			public Category getValue(Category category) {
				return category;
			}
		};

		// Adds action suscription column to table
		getView().getCategoryTable().addColumn(suscriptionColumn, applicationMessages.getGeneralSubscribe());

		// Sets selection model for each row
		final SingleSelectionModel<Category> selectionModel = new SingleSelectionModel<Category>(Category.KEY_PROVIDER);
		getView().getCategoryTable().setSelectionModel(selectionModel);
		selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
			public void onSelectionChange(SelectionChangeEvent event) {

				if (selectionModel.getSelectedObject() != null){
					if (selectedType.equals(SelectionType.SELECTED_CATEGORY)) {
						List<Category> categories = new ArrayList<Category>();
						categories.add(selectionModel.getSelectedObject());
						searchParlamentarian(categories);
						getView().notificationSelectCategory();
						getView().displaySelectionCategory();
					}

					selectedCategory = selectionModel.getSelectedObject();
					getView().setCategoryDisplay(selectedCategory.getName());
					setBillTable();
				}
			}
		});
	}

	public void initBillTable() {
		while (getView().getBillTable().getColumnCount() > 0) {
			getView().getBillTable().removeColumn(0);
		}

		// Creates action view bill column
		Column<Bill, Bill> viewBillColumn = new Column<Bill, Bill>(new ActionCell<Bill>("", new ActionCell.Delegate<Bill>() {

			@Override
			public void execute(Bill bill) {
				PlaceRequest placeRequest = new PlaceRequest(BillPresenter.PLACE);
				placeRequest = placeRequest.with(BillPresenter.PARAM_BILL_ID, bill.getId().toString());
				placeRequest = placeRequest.with(BillPresenter.PARAM_PARLAMENTARIAN_ID, selectedParlamentarian.getId().toString());
				placeManager.revealPlace(placeRequest);
			}
		}) {
			@Override
			public void render(Cell.Context context, Bill value, SafeHtmlBuilder sb) {
				sb.append(new SafeHtml() {

					@Override
					public String asString() {
						return "<div class=\"glassButton\"></div>";
					}
				});
			}
		}) {

			@Override
			public Bill getValue(Bill bill) {
				return bill;
			}
		};

		// Adds action view bill column to table
		getView().getBillTable().addColumn(viewBillColumn, applicationMessages.getGeneralViewMore());

		// Creates bulletin column
		TextColumn<Bill> bulletinColumn = new TextColumn<Bill>() {
			@Override
			public String getValue(Bill bill) {
				return bill.getBulletinNumber();
			}
		};
		// Sets sortable bulletin column
		bulletinColumn.setSortable(true);
		ListHandler<Bill> bulletinSortHandler = new ListHandler<Bill>(((ListDataProvider<Bill>) billData).getList());
		getView().getBillTable().addColumnSortHandler(bulletinSortHandler);
		bulletinSortHandler.setComparator(bulletinColumn, new Comparator<Bill>() {
			public int compare(Bill o1, Bill o2) {
				return o1.getBulletinNumber().compareTo(o2.getBulletinNumber());
			}
		});

		// Adds bulletin column to table
		getView().getBillTable().addColumn(bulletinColumn, applicationMessages.getBillBulletin());

		// Creates title column
		TextColumn<Bill> titleColumn = new TextColumn<Bill>() {
			@Override
			public String getValue(Bill bill) {
				return bill.getTitle();
			}
		};
		// Sets sortable title column
		titleColumn.setSortable(true);
		ListHandler<Bill> titleSortHandler = new ListHandler<Bill>(((ListDataProvider<Bill>) billData).getList());
		getView().getBillTable().addColumnSortHandler(titleSortHandler);
		titleSortHandler.setComparator(titleColumn, new Comparator<Bill>() {
			public int compare(Bill o1, Bill o2) {
				return o1.getTitle().compareTo(o2.getTitle());
			}
		});

		// Adds title column to table
		getView().getBillTable().addColumn(titleColumn, applicationMessages.getBillConflictedBill());

		// Creates isAuthor column
		Column<Bill, String> isAuthorColumn = new Column<Bill, String>(new ImageCell()){
			@Override
			public String getValue(Bill bill) {
				if (selectedParlamentarian != null && selectedParlamentarian.getAuthoredBills() != null) {
					if (selectedParlamentarian.getAuthoredBills().contains(bill)) {
						return "images/shoeprints.png";
					}
				}
				return "images/shoeprints_hidden.png";
			}
		};

		// Adds isAuthor column to table
		getView().getBillTable().addColumn(isAuthorColumn, applicationMessages.getBillIsAuthoredBill());

		// Creates isVoted column
		Column<Bill, String> isVotedColumn = new Column<Bill, String>(new ImageCell()){
			@Override
			public String getValue(Bill bill) {
				if (selectedParlamentarian != null && selectedParlamentarian.getVotedBills() != null) {
					if (selectedParlamentarian.getVotedBills().contains(bill)) {
						return "images/shoeprints.png";
					}
				}
				return "images/shoeprints_hidden.png";
			}
		};

		// Adds isVoted column to table
		getView().getBillTable().addColumn(isVotedColumn, applicationMessages.getBillVotedInChamber());

		// Creates action suscription column
		Column<Bill, Bill> suscriptionColumn = new Column<Bill, Bill>(new ActionCell<Bill>("", new ActionCell.Delegate<Bill>() {

			@Override
			public void execute(Bill bill) {
				// TODO: add bill suscription servlet
				PlaceRequest placeRequest = new PlaceRequest(SubscriptionPresenter.PLACE);
				placeManager.revealPlace(placeRequest.with(SubscriptionPresenter.PARAM_BILL_ID, bill.getId().toString()));
			}
		}) {
			@Override
			public void render(Cell.Context context, Bill value, SafeHtmlBuilder sb) {
				sb.append(new SafeHtml() {

					@Override
					public String asString() {
						return "<div class=\"suscribeButtonBill\"></div>";
					}
				});
			}
		}) {

			@Override
			public Bill getValue(Bill bill) {
				return bill;
			}
		};

		// Adds action subscription column to table
		getView().getBillTable().addColumn(suscriptionColumn, applicationMessages.getGeneralSubscribe());
	}

	public void setBillTable() {
		if (selectedParlamentarian != null && selectedCategory != null) {
			if (selectedType.equals(SelectionType.SELECTED_PARLAMENTARIAN)) {
				getView().displaySelectionCategory();
			}
			else if (selectedType.equals(SelectionType.SELECTED_CATEGORY)) {
				getView().displaySelectionParliamentarian();
			}
			searchBill(selectedParlamentarian.getId(), selectedCategory.getId());
			getView().showBillTable();
		} else {
			setBillData(new ListDataProvider<Bill>());
		}
	}

	@Override
	public void showParlamentarianProfile(){
		if (selectedParlamentarian != null) {
			PlaceRequest placeRequest = new PlaceRequest(ParlamentarianPresenter.PLACE);
			placeManager.revealPlace(placeRequest.with(ParlamentarianPresenter.PARAM_PARLAMENTARIAN_ID, selectedParlamentarian.getId().toString()));
		}
	}

	@Override
	public void resetSelection() {
		selectedParlamentarian = null;
		selectedCategory = null;
		getView().setCategorySearch(applicationMessages.getCategorySearchMessage());
		getView().setParlamentarianSearch(applicationMessages.getParlamentarianSearchMessage());
		getView().setParlamentarianDisplay(applicationMessages.getGeneralParlamentarian());
		getView().setParlamentarianImage("images/parlamentarian/large/avatar.png");
		getView().setCategoryDisplay(applicationMessages.getGeneralCategory());
		getView().hideParlamentarianMessage();
		getView().hideCategoryMessage();
		getView().hideBillMessage();
	}

	@Override
	public void switchSelectionType() {
		if (selectedType.equals(SelectionType.SELECTED_PARLAMENTARIAN)) {
			setupSelection(SelectionType.SELECTED_CATEGORY);
		}
		else if (selectedType.equals(SelectionType.SELECTED_CATEGORY)) {
			setupSelection(SelectionType.SELECTED_PARLAMENTARIAN);
		}
		searchCleaner();
	}

	@Override
	public void searchCleaner() {
		getView().getParlamentarianTable().getSelectionModel().setSelected(selectedParlamentarian, false);
		getView().getCategoryTable().getSelectionModel().setSelected(selectedCategory, false);
		resetSelection();
		initDataLoad();
	}

	@Override
	public void setupSelection(SelectionType changeType) {
		selectedType = changeType;
		getView().setSelectedType(selectedType);
	}

}
