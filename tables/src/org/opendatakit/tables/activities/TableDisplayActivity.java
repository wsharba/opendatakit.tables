package org.opendatakit.tables.activities;

import java.util.ArrayList;

import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.PossibleTableViewTypes;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.R;
import org.opendatakit.tables.fragments.DetailViewFragment;
import org.opendatakit.tables.fragments.GraphManagerFragment;
import org.opendatakit.tables.fragments.GraphViewFragment;
import org.opendatakit.tables.fragments.ListViewFragment;
import org.opendatakit.tables.fragments.MapListViewFragment;
import org.opendatakit.tables.fragments.SpreadsheetFragment;
import org.opendatakit.tables.fragments.TableMapInnerFragment;
import org.opendatakit.tables.fragments.TableMapInnerFragment.TableMapInnerFragmentListener;
import org.opendatakit.tables.utils.ActivityUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.SQLQueryStruct;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

/**
 * Displays information about a table. List, Map, and Detail views are all
 * displayed via this  activity.
 * @author sudar.sam@gmail.com
 *
 */
public class TableDisplayActivity extends AbsTableActivity
    implements TableMapInnerFragmentListener {

  private static final String TAG = TableDisplayActivity.class.getSimpleName();
  private static final String INTENT_KEY_CURRENT_FRAGMENT =
      "saveInstanceCurrentFragment";

  /**
   * The fragment types this activity could be displaying.
   * @author sudar.sam@gmail.com
   *
   */
  public enum ViewFragmentType {
    SPREADSHEET,
    LIST,
    MAP,
    GRAPH_MANAGER,
    GRAPH_VIEW,
    DETAIL;
  }

  /**
   * The {@link UserTable} that is being displayed in this activity.
   */
  private UserTable mUserTable;
  /**
   *  The type of fragment that is currently being displayed.
   */
  private ViewFragmentType mCurrentFragmentType;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // see if we saved the state
    this.initializeBackingTable();
    this.mCurrentFragmentType =
        this.retrieveFragmentTypeToDisplay(savedInstanceState);
    this.setContentView(R.layout.activity_table_display_activity);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    this.mCurrentFragmentType =
        this.retrieveFragmentTypeToDisplay(savedInstanceState);
    Log.i(TAG, "[onRestoreInstanceState] current fragment type: " +
        this.mCurrentFragmentType);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (this.mCurrentFragmentType != null) {
      Log.i(TAG, "[onSaveInstanceState] saving current fragment type: "
          + this.mCurrentFragmentType.name());
      outState.putString(
          INTENT_KEY_CURRENT_FRAGMENT,
          this.mCurrentFragmentType.name());
    } else {
      Log.i(TAG, "[onSaveInstanceState] no current fragment type to save");
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.i(TAG, "[onResume]");
    this.initializeDisplayFragment();
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // clear the menu so that we don't double inflate
    menu.clear();
    MenuInflater menuInflater = this.getMenuInflater();
    switch (this.getCurrentFragmentType()) {
    case SPREADSHEET:
    case LIST:
    case GRAPH_MANAGER:
    case MAP:
      menuInflater.inflate(
          R.menu.top_level_table_menu,
          menu);
      PossibleTableViewTypes viewTypes = this.retrievePossibleViewTypes();
      this.enableAndDisableViewTypes(viewTypes, menu);
      this.selectCorrectViewType(menu);
      break;
    case DETAIL:
      menuInflater.inflate(R.menu.detail_view_menu, menu);
      break;
    case GRAPH_VIEW:
      // for now, do nothing.
      break;
    }
    return super.onCreateOptionsMenu(menu);
  }
  
  /**
   * Retrieve the {@link PossibleTableViewTypes} representing the valid views
   * for this table.
   * @return
   */
  PossibleTableViewTypes retrievePossibleViewTypes() {
    return this.getTableProperties().getPossibleViewTypes();
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.top_level_table_menu_view_spreadsheet_view:
      this.showSpreadsheetFragment();
      return true;
    case R.id.top_level_table_menu_view_list_view:
      this.showListFragment();
      return true;
    case R.id.top_level_table_menu_view_graph_view:
      this.showGraphFragment();
      return true;
    case R.id.top_level_table_menu_view_map_view:
      this.showMapFragment();
      return true;
    case R.id.top_level_table_menu_add:
      Log.d(TAG, "[onOptionsItemSelected] add selected");
      ActivityUtil.addRow(
          this,
          this.getTableProperties(),
          null);
      return true;
    case R.id.top_level_table_menu_table_properties:
      ActivityUtil.launchTableLevelPreferencesActivity(
          this,
          this.getAppName(),
          this.getTableProperties().getTableId(),
          TableLevelPreferencesActivity.FragmentType.TABLE_PREFERENCE);
      return true;
    case R.id.menu_edit_row:
      // We need to retrieve the row id.
      DetailViewFragment detailViewFragment = this.findDetailViewFragment();
      if (detailViewFragment == null) {
        Log.e(
            TAG,
            "[onOptionsItemSelected] trying to edit row, but detail view " +
              " fragment null");
        Toast.makeText(
            this,
            getString(R.string.cannot_edit_row_please_try_again),
            Toast.LENGTH_LONG)
          .show();
      }
      String rowId = detailViewFragment.getRowId();
      if (rowId == null) {
        Log.e(
            TAG,
            "[onOptionsItemSelected trying to edit row, but row id is null");
        Toast.makeText(
            this,
            getString(R.string.cannot_edit_row_please_try_again),
            Toast.LENGTH_LONG)
          .show();
      }
      ActivityUtil.editRow(
          this,
          this.getTableProperties(),
          rowId);
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }
  
  @Override
  protected void onActivityResult(
      int requestCode,
      int resultCode,
      Intent data) {
    switch (requestCode) {
    // For now, we will just refresh the table if something could have changed.
    case Constants.RequestCodes.ADD_ROW_COLLECT:
    case Constants.RequestCodes.ADD_ROW_SURVEY:
    case Constants.RequestCodes.EDIT_ROW_COLLECT:
    case Constants.RequestCodes.EDIT_ROW_SURVEY:
      if (resultCode == Activity.RESULT_OK) {
        Log.d(TAG, "[onActivityResult] result ok, refreshing backing table");
        this.refreshDataTable();
        // We also want to cause the fragments to redraw themselves, as their
        // data may have changed.
        this.refreshDisplayFragment();
      } else {
        Log.d(
            TAG,
            "[onActivityResult] result canceled, not refreshing backing " +
              "table");
      }
      break;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }
  
  /**
   * Disable or enable those menu items corresponding to view types that are
   * currently invalid or valid, respectively. The inflatedMenu must have
   * already been created from the resource.
   * @param validViewTypes
   * @param inflatedMenu
   */
  private void enableAndDisableViewTypes(
      PossibleTableViewTypes possibleViews,
      Menu inflatedMenu) {
    MenuItem spreadsheetItem = inflatedMenu.findItem(
        R.id.top_level_table_menu_view_spreadsheet_view);
    MenuItem listItem = inflatedMenu.findItem(
        R.id.top_level_table_menu_view_list_view);
    MenuItem mapItem = inflatedMenu.findItem(
        R.id.top_level_table_menu_view_map_view);
    MenuItem graphItem = inflatedMenu.findItem(
        R.id.top_level_table_menu_view_graph_view);
    spreadsheetItem.setEnabled(possibleViews.spreadsheetViewIsPossible());
    listItem.setEnabled(possibleViews.listViewIsPossible());
    mapItem.setEnabled(possibleViews.mapViewIsPossible());
    graphItem.setEnabled(possibleViews.graphViewIsPossible());
  }
  
  /**
   * Selects the correct view type that is being displayed by the
   * {@link ITopLevelTableMenuActivity}.
   * @param impl
   * @param inflatedMenu
   */
  private void selectCorrectViewType(Menu inflatedMenu) {
    ViewFragmentType currentFragment = this.getCurrentFragmentType();
    if (currentFragment == null) {
      Log.e(TAG, "did not find a current fragment type. Not selecting view.");
      return;
    }
    MenuItem menuItem = null;
    switch (currentFragment) {
    case SPREADSHEET:
      menuItem = inflatedMenu.findItem(
          R.id.top_level_table_menu_view_spreadsheet_view);
      menuItem.setChecked(true);
      break;
    case LIST:
      menuItem = inflatedMenu.findItem(
          R.id.top_level_table_menu_view_list_view);
      menuItem.setChecked(true);
      break;
    case GRAPH_MANAGER:
      menuItem = inflatedMenu.findItem(
          R.id.top_level_table_menu_view_graph_view);
      menuItem.setChecked(true);
      break;
    case MAP:
      menuItem = inflatedMenu.findItem(
          R.id.top_level_table_menu_view_map_view);
      menuItem.setChecked(true);
      break;
    default:
      Log.e(TAG, "view type not recognized: " + currentFragment);
    }
  }

  @Override
  protected void onStart() {
     super.onStart();
     Log.i(TAG, "[onStart]");
  }
  
  protected void refreshDisplayFragment() {
    this.helperInitializeDisplayFragment(true);
  }
  
  protected void initializeDisplayFragment() {
    this.helperInitializeDisplayFragment(false);
  }

  /**
   * Initialize the correct display fragment based on the result of
   * {@link #retrieveTableIdFromIntent()}. Initializes Spreadsheet if none
   * is present in Intent.
   */
  private void helperInitializeDisplayFragment(boolean createNew) {
    switch (this.mCurrentFragmentType) {
    case SPREADSHEET:
      this.showSpreadsheetFragment(createNew);
      break;
    case DETAIL:
      this.showDetailFragment(createNew);
      break;
    case GRAPH_MANAGER:
      this.showGraphFragment(createNew);
      break;
    case LIST:
      this.showListFragment(createNew);
      break;
    case MAP:
      this.showMapFragment(createNew);
      break;
    case GRAPH_VIEW:
      String graphName =
          this.getIntent().getStringExtra(Constants.IntentKeys.GRAPH_NAME);
      if (graphName == null) {
        Log.e(
            TAG,
            "[initializeDisplayFragment] graph name not present in bundle");
      }
      this.showGraphViewFragment(graphName, createNew);
      break;
    default:
      Log.e(TAG, "ViewFragmentType not recognized: " +
          this.mCurrentFragmentType);
      break;
    }
  }

  /**
   * Set the current type of fragment that is being displayed.
   * @param currentType
   */
  protected void setCurrentFragmentType(ViewFragmentType currentType) {
    this.mCurrentFragmentType = currentType;
  }

  /**
   * @return the {@link ViewFragmentType} that was passed in the intent,
   * or null if none exists.
   */
  protected ViewFragmentType retrieveViewFragmentTypeFromIntent() {
    if (this.getIntent().getExtras() == null) {
      return null;
    }
    String viewFragmentTypeStr = this.getIntent().getExtras().getString(
        Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE);
    if (viewFragmentTypeStr == null) {
      return null;
    } else {
      ViewFragmentType result = ViewFragmentType.valueOf(viewFragmentTypeStr);
      return result;
    }
  }

  /**
   * Get the {@link ViewFragmentType} that should be displayed. Any type in
   * the passed in bundle takes precedence, on the assumption that is was from
   * a saved instance state. Next is any type that
   * was passed in the Intent. If neither is present, the value
   * corresponding to {@link TableProperties#getDefaultViewType()} wins. If
   * none is present, returns {@link ViewFragmentType#SPREADSHEET}.
   * @return
   */
  protected ViewFragmentType retrieveFragmentTypeToDisplay(
      Bundle savedInstanceState) {
    // 1) First check the passed in bundle.
    if (savedInstanceState != null &&
        savedInstanceState.containsKey(INTENT_KEY_CURRENT_FRAGMENT)) {
      String instanceTypeStr =
          savedInstanceState.getString(INTENT_KEY_CURRENT_FRAGMENT);
      Log.i(TAG, "[retrieveFragmentTypeToDisplay] found type in saved instance" +
          " state: " + instanceTypeStr);
      return ViewFragmentType.valueOf(instanceTypeStr);
    }
    Log.i(TAG, "[retrieveFragmentTypeToDisplay] didn't find fragment type " +
    		"in saved instance state");
    // 2) then check the intent
    ViewFragmentType result = retrieveViewFragmentTypeFromIntent();
    if (result == null) {
      // 3) then use the default
      TableViewType viewType =
          this.getTableProperties().getDefaultViewType();
      result = this.getViewFragmentTypeFromViewType(viewType);
    }
    if (result == null) {
      // 4) last case, do spreadsheet
      Log.i(TAG, "[retrieveFragmentTypeToDisplay] no view type found, " +
      		"defaulting to spreadsheet");
      result = ViewFragmentType.SPREADSHEET;
    }
    return result;
  }

  /**
   * Get the {@link ViewFragmentType} that corresponds to
   * {@link TableViewType}. If no match is found, returns null.
   * @param viewType
   * @return
   */
  public ViewFragmentType getViewFragmentTypeFromViewType(
      TableViewType viewType) {
    switch (viewType) {
    case SPREADSHEET:
      return ViewFragmentType.SPREADSHEET;
    case MAP:
      return ViewFragmentType.MAP;
    case GRAPH:
      return ViewFragmentType.GRAPH_MANAGER;
    case LIST:
      return ViewFragmentType.LIST;
    default:
      Log.e(TAG, "viewType " + viewType + " not recognized.");
      return null;
    }
  }

  /**
   * Initialize {@link TableDisplayActivity#mUserTable}.
   */
  private void initializeBackingTable() {
    UserTable userTable = this.retrieveUserTable();
    this.mUserTable = userTable;
  }

  /**
   * Get the {@link UserTable} that is being held by this activity.
   * @return
   */
  public UserTable getUserTable() {
    return this.mUserTable;
  }

  /**
   * Refresh the data being displayed.
   */
  public void refreshDataTable() {
    this.initializeBackingTable();
  }

  /**
   * Get the {@link UserTable} from the database that should be displayed.
   * @return
   */
  UserTable retrieveUserTable() {
    TableProperties tableProperties = this.getTableProperties();
    SQLQueryStruct sqlQueryStruct =
        this.retrieveSQLQueryStatStructFromIntent();
    DbTable dbTable = DbTable.getDbTable(tableProperties);
    UserTable result = dbTable.rawSqlQuery(
        sqlQueryStruct.whereClause,
        sqlQueryStruct.selectionArgs,
        sqlQueryStruct.groupBy,
        sqlQueryStruct.having,
        sqlQueryStruct.orderByElementKey,
        sqlQueryStruct.orderByDirection);
    return result;
  }

  /**
   * Retrieve the {@link SQLQueryStruct} specified in the {@link Intent} that
   * restricts the current table.
   * @return
   */
  SQLQueryStruct retrieveSQLQueryStatStructFromIntent() {
    SQLQueryStruct result = IntentUtil.getSQLQueryStructFromBundle(
        this.getIntent().getExtras());
    return result;
  }
  
  /**
   * Show the spreadsheet fragment, creating a new one if it doesn't yet exist.
   */
  public void showSpreadsheetFragment() {
    this.showSpreadsheetFragment(false);
  }

  /**
   * Show the spreadsheet fragment.
   * @param createNew
   */
  public void showSpreadsheetFragment(boolean createNew) {
    this.setCurrentFragmentType(ViewFragmentType.SPREADSHEET);
    this.updateChildViewVisibility(ViewFragmentType.SPREADSHEET);
    FragmentManager fragmentManager = this.getFragmentManager();
    // Try to retrieve one already there.
    SpreadsheetFragment spreadsheetFragment = (SpreadsheetFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.SPREADSHEET);
    if (spreadsheetFragment == null || createNew) {
      spreadsheetFragment = this.createSpreadsheetFragment();
    }
    FragmentTransaction fragmentTransaction =
        fragmentManager.beginTransaction();
    fragmentTransaction.replace(
        R.id.activity_table_display_activity_one_pane_content,
        spreadsheetFragment,
        Constants.FragmentTags.SPREADSHEET);
    fragmentTransaction.commit();
  }
  
  /**
   * Create a {@link SpreadsheetFragment} to be displayed in the activity.
   * @return
   */
  SpreadsheetFragment createSpreadsheetFragment() {
    SpreadsheetFragment result = new SpreadsheetFragment();
    return result;
  }
  
  public void showMapFragment() {
    this.showMapFragment(false);
  }

  public void showMapFragment(boolean createNew) {
    this.setCurrentFragmentType(ViewFragmentType.MAP);
    this.updateChildViewVisibility(ViewFragmentType.MAP);
    // Set the list view file name.
    String fileName =
        IntentUtil.retrieveFileNameFromBundle(this.getIntent().getExtras());
    if (fileName == null) {
      // use the default.
      fileName = this.getTableProperties().getMapListViewFileName();
    }
    FragmentManager fragmentManager = this.getFragmentManager();
    MapListViewFragment mapListViewFragment = (MapListViewFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.MAP_LIST);
    TableMapInnerFragment innerMapFragment = (TableMapInnerFragment)
        fragmentManager.findFragmentByTag(
            Constants.FragmentTags.MAP_INNER_MAP);
    if (mapListViewFragment == null || createNew) {
      Log.d(TAG, "[showMapFragment] creating new map list fragment");
      mapListViewFragment = this.createMapListViewFragment(fileName);
    } else {
      Log.d(TAG, "[showMapFragment] existing map list fragment found");
    }
    if (innerMapFragment == null || createNew) {
      Log.d(
          TAG,
          "[showMapFragment] creating new inner map fragment");
      innerMapFragment = this.createInnerMapFragment();
    } else {
      Log.d(TAG, "[showMapFragment] existing inner map fragment found");
    }
    innerMapFragment.listener = this;
    FragmentTransaction fragmentTransaction =
        fragmentManager.beginTransaction();
    fragmentTransaction
        .replace(
            R.id.map_view_list,
            mapListViewFragment,
            Constants.FragmentTags.MAP_LIST)
        .replace(
            R.id.map_view_inner_map,
            innerMapFragment,
            Constants.FragmentTags.MAP_INNER_MAP);
    fragmentTransaction.commit();
  }
  
  /**
   * Create the {@link TableMapInnerFragment} that will be displayed as the
   * map.
   * @return
   */
  TableMapInnerFragment createInnerMapFragment() {
    TableMapInnerFragment result = new TableMapInnerFragment();
    return result;
  }
  
  /**
   * Create the {@link MapListViewFragment} that will be displayed with the
   * map view.
   * @param listViewFileName the file name of the list view that will be
   * displayed
   * @return
   */
  MapListViewFragment createMapListViewFragment(String listViewFileName) {
    MapListViewFragment result = new MapListViewFragment();
    Bundle listArguments = new Bundle();
    IntentUtil.addFileNameToBundle(listArguments, listViewFileName);
    result.setArguments(listArguments);
    return result;
  }
  
  public void showListFragment() {
    this.showListFragment(false);
  }

  public void showListFragment(boolean createNew) {
    this.setCurrentFragmentType(ViewFragmentType.LIST);
    this.updateChildViewVisibility(ViewFragmentType.LIST);
    // Try to use a passed file name. If one doesn't exist, try to use the
    // default.
    String fileName =
        IntentUtil.retrieveFileNameFromBundle(this.getIntent().getExtras());
    if (fileName == null) {
      fileName = getTableProperties().getListViewFileName();
    }
    FragmentManager fragmentManager = this.getFragmentManager();
    ListViewFragment listViewFragment = (ListViewFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.LIST);
    if (listViewFragment == null || createNew) {
      listViewFragment = this.createListViewFragment(fileName);
    } 
    FragmentTransaction fragmentTransaction =
        fragmentManager.beginTransaction();
    fragmentTransaction.replace(
        R.id.activity_table_display_activity_one_pane_content,
        listViewFragment,
        Constants.FragmentTags.LIST);
    fragmentTransaction.commit();
  }
  
  /**
   * Create a {@link ListViewFragment} to be used by the activity.
   * @param fileName the file name to be displayed
   */
  ListViewFragment createListViewFragment(String fileName) {
    ListViewFragment result = new ListViewFragment();
    Bundle arguments = new Bundle();
    IntentUtil.addFileNameToBundle(arguments, fileName);
    result.setArguments(arguments);
    return result;
  }
  
  public void showGraphFragment() {
    this.showGraphFragment(false);
  }

  public void showGraphFragment(boolean createNew) {
    this.setCurrentFragmentType(ViewFragmentType.GRAPH_MANAGER);
    this.updateChildViewVisibility(ViewFragmentType.GRAPH_MANAGER);
    FragmentManager fragmentManager = this.getFragmentManager();
    // Try to retrieve the fragment if it already exists.
    GraphManagerFragment graphManagerFragment = (GraphManagerFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.GRAPH_MANAGER);
    if (graphManagerFragment == null || createNew) {
      graphManagerFragment = this.createGraphManagerFragment();
    }
    FragmentTransaction fragmentTransaction =
        fragmentManager.beginTransaction();
    fragmentTransaction.replace(
        R.id.activity_table_display_activity_one_pane_content,
        graphManagerFragment,
        Constants.FragmentTags.GRAPH_MANAGER);
    fragmentTransaction.commit();
  }
  
  /**
   * Create a {@link GraphManagerFragment} that will be used by the activity.
   * @return
   */
  GraphManagerFragment createGraphManagerFragment() {
    GraphManagerFragment result = new GraphManagerFragment();
    return result;
  }
  
  public void showGraphViewFragment(String graphName) {
    this.showGraphViewFragment(graphName, false);
  }

  public void showGraphViewFragment(String graphName, boolean createNew) {
    Log.d(TAG, "[showGraphViewFragment] graph name: " + graphName);
    this.setCurrentFragmentType(ViewFragmentType.GRAPH_VIEW);
    this.updateChildViewVisibility(ViewFragmentType.GRAPH_VIEW);
    // Try and use the default.
    FragmentManager fragmentManager = this.getFragmentManager();
    GraphViewFragment graphViewFragment = (GraphViewFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.GRAPH_VIEW);
    if (graphViewFragment == null || createNew) {
      graphViewFragment = this.createGraphViewFragment(graphName);
    } else {
      // Add the value to the existing fragment so it displays the correct
      // value.
      Bundle arguments = new Bundle();
      arguments.putString(Constants.IntentKeys.GRAPH_NAME, graphName);
      graphViewFragment.getArguments().putAll(arguments);
    }
    FragmentTransaction fragmentTransaction =
        fragmentManager.beginTransaction();
    fragmentTransaction.replace(
        R.id.activity_table_display_activity_one_pane_content,
        graphViewFragment,
        Constants.FragmentTags.GRAPH_VIEW);
    fragmentTransaction.commit();
  }
  
  /**
   * Create a {@link GraphViewFragment} to be added to the activity.
   * @param graphName
   * @return
   */
  GraphViewFragment createGraphViewFragment(String graphName) {
    GraphViewFragment result = new GraphViewFragment();
    Bundle arguments = new Bundle();
    arguments.putString(Constants.IntentKeys.GRAPH_NAME, graphName);
    result.setArguments(arguments);
    return result;
  }
  
  public void showDetailFragment() {
    this.showDetailFragment(false);
  }

  public void showDetailFragment(boolean createNew) {
    this.setCurrentFragmentType(ViewFragmentType.DETAIL);
    this.updateChildViewVisibility(ViewFragmentType.DETAIL);
    FragmentManager fragmentManager = this.getFragmentManager();
    String fileName = IntentUtil.retrieveFileNameFromBundle(
        this.getIntent().getExtras());
    // Try and use the default.
    if (fileName == null) {
      Log.d(TAG, "[showDetailFragment] fileName not found in Intent");
      fileName = this.getTableProperties().getDetailViewFileName();
    }
    String rowId = IntentUtil.retrieveRowIdFromBundle(
        this.getIntent().getExtras());
    // Try to retrieve one that already exists.
    DetailViewFragment detailViewFragment = (DetailViewFragment)
        fragmentManager.findFragmentByTag(
            Constants.FragmentTags.DETAIL_FRAGMENT);
    if (detailViewFragment == null || createNew) {
      detailViewFragment = this.createDetailViewFragment(fileName, rowId);
    }
    FragmentTransaction fragmentTransaction =
        fragmentManager.beginTransaction();
    fragmentTransaction.replace(
        R.id.activity_table_display_activity_one_pane_content,
        detailViewFragment,
        Constants.FragmentTags.DETAIL_FRAGMENT);
    fragmentTransaction.commit();
  }
  
  /**
   * Create a {@link DetailViewFragment} to be used with the fragments.
   * @param fileName
   * @param rowId
   * @return
   */
  DetailViewFragment createDetailViewFragment(String fileName, String rowId) {
    DetailViewFragment result = new DetailViewFragment();
    Bundle bundle = new Bundle();
    IntentUtil.addRowIdToBundle(bundle, rowId);
    IntentUtil.addFileNameToBundle(bundle, fileName);
    result.setArguments(bundle);
    return result;
  }
  
  /**
   * Update the content view's children visibility for viewFragmentType. This
   * is required due to the fact that not all the fragments make use of the
   * same children views within the activity.
   * @param viewFragmentType
   */
  void updateChildViewVisibility(ViewFragmentType viewFragmentType) {
    // The map fragments occupy a different view than the single pane
    // content. This is because the map is two views--the list and the map
    // itself. So, we need to hide and show the others as appropriate.
    View onePaneContent = this.findViewById(
        R.id.activity_table_display_activity_one_pane_content);
    View mapContent =
        this.findViewById(R.id.activity_table_display_activity_map_content);
    switch (viewFragmentType) {
    case DETAIL:
    case GRAPH_MANAGER:
    case GRAPH_VIEW:
    case LIST:
    case SPREADSHEET:
      onePaneContent.setVisibility(View.VISIBLE);
      mapContent.setVisibility(View.GONE);
      return;
    case MAP:
      onePaneContent.setVisibility(View.GONE);
      mapContent.setVisibility(View.VISIBLE);
      return;
    default:
      Log.e(
          TAG,
          "[updateChildViewVisibility] unrecognized type: " +
              viewFragmentType);
    }
  }

  /**
   * Retrieve the {@link DetailViewFragment} that is associated with this
   * activity.
   * @return the fragment, or null if it is not present
   */
  DetailViewFragment findDetailViewFragment() {
    FragmentManager fragmentManager = this.getFragmentManager();
    DetailViewFragment result = (DetailViewFragment)
        fragmentManager.findFragmentByTag(
            Constants.FragmentTags.DETAIL_FRAGMENT);
    return result;
  }


  /**
   * Return the {@link ViewFragmentType} that is currently being displayed.
   */
  public ViewFragmentType getCurrentFragmentType() {
    return this.mCurrentFragmentType;
  }

  @Override
  public void onHideList() {
    Log.d(TAG, "[onHideList] called. Not set up.");
//    View view = this.findViewById(R.id.map_view_list);
//    view.setVisibility(View.GONE);
  }

  @Override
  public void onSetIndex(int i) {
    MapListViewFragment mapListViewFragment = this.findMapListViewFragment();
    if (mapListViewFragment == null) {
      Log.e(TAG, "[onSetIndex] mapListViewFragment is null! Returning");
      return;
    } else {
      mapListViewFragment.setMapListIndex(i);
    }
  }

  @Override
  public void onSetInnerIndexes(ArrayList<Integer> indexes) {
    MapListViewFragment mapListViewFragment = this.findMapListViewFragment();
    if (mapListViewFragment == null) {
      Log.e(TAG, "[onSetInnerIndexes] fragment is null! Returning");
    } else {
      mapListViewFragment.setMapListIndices(indexes);
    }
  }
  
  /**
   * Find a {@link MapListViewFragment} that is associated with this activity.
   * If not present, returns null.
   * @return
   */
  MapListViewFragment findMapListViewFragment() {
    FragmentManager fragmentManager = this.getFragmentManager();
    MapListViewFragment result = (MapListViewFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.MAP_LIST);
    return result;
  }
  
}