package dulinglai.android.alode.resources.androidConstants;

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AndroidSootClassConstants {

    public static final Set<String> SET_LISTENER_METHODS = new HashSet<>(Arrays.asList(
            "setOnClickListener",
            "setOnContextClickListener",
            "setOnCreateContextMenuListener",
            "setOnDragListener",
            "setOnFocusChangeListener",
            "setOnGenericMotionListener",
            "setOnKeyListener",
            "setOnLongClickListener",
            "setOnTouchListener"
            )

    );

    public static final Set<String> WIDGET_CALLBACK_METHODS = new HashSet<>(Arrays.asList(
       "onClick",
       "onItemClick",
       "onContextClick",
       "onKey"
    ));

    public static final Map<String, String> WIDGET_MAP = ImmutableMap.<String, String>builder()
            .put("EditText","android.widget.EditText")
            .put("PointerIcon","android.view.PointerIcon")
            .put("Checkable","android.widget.Checkable")
            .put("MenuInflater","android.view.MenuInflater")
            .put("MediaController","android.widget.MediaController")
            .put("View$OnHoverListener","android.view.View$OnHoverListener")
            .put("ViewStub","android.view.ViewStub")
            .put("ScaleGestureDetector$OnScaleGestureListener","android.view.ScaleGestureDetector$OnScaleGestureListener")
            .put("Menu","android.view.Menu")
            .put("TabHost$OnTabChangeListener","android.widget.TabHost$OnTabChangeListener")
            .put("StackView","android.widget.StackView")
            .put("ArrayAdapter","android.widget.ArrayAdapter")
            .put("ActionMenuView$LayoutParams","android.widget.ActionMenuView$LayoutParams")
            .put("ExpandableListView","android.widget.ExpandableListView")
            .put("CaptioningManager","android.view.accessibility.CaptioningManager")
            .put("AnimationUtils","android.view.animation.AnimationUtils")
            .put("AdapterView","android.widget.AdapterView")
            .put("RatingBar$OnRatingBarChangeListener","android.widget.RatingBar$OnRatingBarChangeListener")
            .put("TextServicesManager","android.view.textservice.TextServicesManager")
            .put("MediaController$MediaPlayerControl","android.widget.MediaController$MediaPlayerControl")
            .put("TimePicker$OnTimeChangedListener","android.widget.TimePicker$OnTimeChangedListener")
            .put("InputMethodSession","android.view.inputmethod.InputMethodSession")
            .put("SpellCheckerSession","android.view.textservice.SpellCheckerSession")
            .put("View$MeasureSpec","android.view.View$MeasureSpec")
            .put("LinearLayout$LayoutParams","android.widget.LinearLayout$LayoutParams")
            .put("Space","android.widget.Space")
            .put("InputDevice","android.view.InputDevice")
            .put("LayoutInflater$Filter","android.view.LayoutInflater$Filter")
            .put("SoundEffectConstants","android.view.SoundEffectConstants")
            .put("SimpleCursorTreeAdapter","android.widget.SimpleCursorTreeAdapter")
            .put("SeekBar$OnSeekBarChangeListener","android.widget.SeekBar$OnSeekBarChangeListener")
            .put("HeterogeneousExpandableList","android.widget.HeterogeneousExpandableList")
            .put("ViewTreeObserver$OnTouchModeChangeListener","android.view.ViewTreeObserver$OnTouchModeChangeListener")
            .put("Animation$AnimationListener","android.view.animation.Animation$AnimationListener")
            .put("Gallery","android.widget.Gallery")
            .put("ZoomControls","android.widget.ZoomControls")
            .put("ViewTreeObserver$OnPreDrawListener","android.view.ViewTreeObserver$OnPreDrawListener")
            .put("ViewManager","android.view.ViewManager")
            .put("Display","android.view.Display")
            .put("WindowManager$LayoutParams","android.view.WindowManager$LayoutParams")
            .put("WindowContentFrameStats","android.view.WindowContentFrameStats")
            .put("SectionIndexer","android.widget.SectionIndexer")
            .put("ContextMenu","android.view.ContextMenu")
            .put("BaseExpandableListAdapter","android.widget.BaseExpandableListAdapter")
            .put("InputDevice$MotionRange","android.view.InputDevice$MotionRange")
            .put("AbsSavedState","android.view.AbsSavedState")
            .put("LinearInterpolator","android.view.animation.LinearInterpolator")
            .put("ViewDebug","android.view.ViewDebug")
            .put("AnalogClock","android.widget.AnalogClock")
            .put("RadioButton","android.widget.RadioButton")
            .put("HeaderViewListAdapter","android.widget.HeaderViewListAdapter")
            .put("AbsSeekBar","android.widget.AbsSeekBar")
            .put("AnticipateInterpolator","android.view.animation.AnticipateInterpolator")
            .put("TextClock","android.widget.TextClock")
            .put("GestureDetector","android.view.GestureDetector")
            .put("Window$OnFrameMetricsAvailableListener","android.view.Window$OnFrameMetricsAvailableListener")
            .put("ViewDebug$HierarchyTraceType","android.view.ViewDebug$HierarchyTraceType")
            .put("SpinnerAdapter","android.widget.SpinnerAdapter")
            .put("GestureDetector$OnContextClickListener","android.view.GestureDetector$OnContextClickListener")
            .put("TabWidget","android.widget.TabWidget")
            .put("NumberPicker$OnScrollListener","android.widget.NumberPicker$OnScrollListener")
            .put("AbsListView$MultiChoiceModeListener","android.widget.AbsListView$MultiChoiceModeListener")
            .put("TableRow$LayoutParams","android.widget.TableRow$LayoutParams")
            .put("ViewSwitcher$ViewFactory","android.widget.ViewSwitcher$ViewFactory")
            .put("GridLayout$LayoutParams","android.widget.GridLayout$LayoutParams")
            .put("Chronometer$OnChronometerTickListener","android.widget.Chronometer$OnChronometerTickListener")
            .put("InputQueue$Callback","android.view.InputQueue$Callback")
            .put("View$OnLayoutChangeListener","android.view.View$OnLayoutChangeListener")
            .put("MultiAutoCompleteTextView$Tokenizer","android.widget.MultiAutoCompleteTextView$Tokenizer")
            .put("SimpleExpandableListAdapter","android.widget.SimpleExpandableListAdapter")
            .put("TableLayout$LayoutParams","android.widget.TableLayout$LayoutParams")
            .put("AccelerateDecelerateInterpolator","android.view.animation.AccelerateDecelerateInterpolator")
            .put("TextView","android.widget.TextView")
            .put("WindowManager$InvalidDisplayException","android.view.WindowManager$InvalidDisplayException")
            .put("KeyCharacterMap","android.view.KeyCharacterMap")
            .put("TabHost","android.widget.TabHost")
            .put("CursorAnchorInfo","android.view.inputmethod.CursorAnchorInfo")
            .put("ViewAnimationUtils","android.view.ViewAnimationUtils")
            .put("Spinner","android.widget.Spinner")
            .put("LayoutAnimationController","android.view.animation.LayoutAnimationController")
            .put("ScrollView","android.widget.ScrollView")
            .put("AccessibilityManager$TouchExplorationStateChangeListener","android.view.accessibility.AccessibilityManager$TouchExplorationStateChangeListener")
            .put("HapticFeedbackConstants","android.view.HapticFeedbackConstants")
            .put("ActionMode$Callback","android.view.ActionMode$Callback")
            .put("Window$OnRestrictedCaptionAreaChangedListener","android.view.Window$OnRestrictedCaptionAreaChangedListener")
            .put("View$OnLongClickListener","android.view.View$OnLongClickListener")
            .put("RatingBar","android.widget.RatingBar")
            .put("View$OnContextClickListener","android.view.View$OnContextClickListener")
            .put("TableLayout","android.widget.TableLayout")
            .put("AlphaAnimation","android.view.animation.AlphaAnimation")
            .put("AbsSpinner","android.widget.AbsSpinner")
            .put("AdapterViewFlipper","android.widget.AdapterViewFlipper")
            .put("ExpandableListView$OnGroupCollapseListener","android.widget.ExpandableListView$OnGroupCollapseListener")
            .put("OrientationListener","android.view.OrientationListener")
            .put("BaseInterpolator","android.view.animation.BaseInterpolator")
            .put("CycleInterpolator","android.view.animation.CycleInterpolator")
            .put("TextView$SavedState","android.widget.TextView$SavedState")
            .put("DigitalClock","android.widget.DigitalClock")
            .put("WindowId$FocusObserver","android.view.WindowId$FocusObserver")
            .put("FrameStats","android.view.FrameStats")
            .put("InputMethod$SessionCallback","android.view.inputmethod.InputMethod$SessionCallback")
            .put("GestureDetector$OnDoubleTapListener","android.view.GestureDetector$OnDoubleTapListener")
            .put("AccessibilityManager","android.view.accessibility.AccessibilityManager")
            .put("View$OnDragListener","android.view.View$OnDragListener")
            .put("View$OnAttachStateChangeListener","android.view.View$OnAttachStateChangeListener")
            .put("KeyEvent","android.view.KeyEvent")
            .put("AdapterViewAnimator","android.widget.AdapterViewAnimator")
            .put("SlidingDrawer$OnDrawerOpenListener","android.widget.SlidingDrawer$OnDrawerOpenListener")
            .put("MotionEvent$PointerCoords","android.view.MotionEvent$PointerCoords")
            .put("GridLayout$Spec","android.widget.GridLayout$Spec")
            .put("LayoutInflater$Factory2","android.view.LayoutInflater$Factory2")
            .put("InputEvent","android.view.InputEvent")
            .put("ExpandableListAdapter","android.widget.ExpandableListAdapter")
            .put("CaptioningManager$CaptionStyle","android.view.accessibility.CaptioningManager$CaptionStyle")
            .put("MotionEvent$PointerProperties","android.view.MotionEvent$PointerProperties")
            .put("DatePicker$OnDateChangedListener","android.widget.DatePicker$OnDateChangedListener")
            .put("Toolbar$LayoutParams","android.widget.Toolbar$LayoutParams")
            .put("RemoteViews$RemoteView","android.widget.RemoteViews$RemoteView")
            .put("ActionMode","android.view.ActionMode")
            .put("ViewGroup$LayoutParams","android.view.ViewGroup$LayoutParams")
            .put("SearchView$OnSuggestionListener","android.widget.SearchView$OnSuggestionListener")
            .put("AccessibilityNodeInfo$RangeInfo","android.view.accessibility.AccessibilityNodeInfo$RangeInfo")
            .put("ExpandableListView$OnChildClickListener","android.widget.ExpandableListView$OnChildClickListener")
            .put("PopupMenu$OnMenuItemClickListener","android.widget.PopupMenu$OnMenuItemClickListener")
            .put("AutoCompleteTextView$Validator","android.widget.AutoCompleteTextView$Validator")
            .put("AccessibilityManager$AccessibilityStateChangeListener","android.view.accessibility.AccessibilityManager$AccessibilityStateChangeListener")
            .put("AccelerateInterpolator","android.view.animation.AccelerateInterpolator")
            .put("Toolbar","android.widget.Toolbar")
            .put("ZoomButtonsController","android.widget.ZoomButtonsController")
            .put("MotionEvent","android.view.MotionEvent")
            .put("CorrectionInfo","android.view.inputmethod.CorrectionInfo")
            .put("Adapter","android.widget.Adapter")
            .put("SimpleCursorAdapter","android.widget.SimpleCursorAdapter")
            .put("SurfaceView","android.view.SurfaceView")
            .put("LayoutAnimationController$AnimationParameters","android.view.animation.LayoutAnimationController$AnimationParameters")
            .put("RadioGroup$OnCheckedChangeListener","android.widget.RadioGroup$OnCheckedChangeListener")
            .put("View$OnClickListener","android.view.View$OnClickListener")
            .put("EdgeEffect","android.widget.EdgeEffect")
            .put("ResourceCursorAdapter","android.widget.ResourceCursorAdapter")
            .put("ViewTreeObserver$OnGlobalFocusChangeListener","android.view.ViewTreeObserver$OnGlobalFocusChangeListener")
            .put("PathInterpolator","android.view.animation.PathInterpolator")
            .put("View$OnTouchListener","android.view.View$OnTouchListener")
            .put("ExpandableListView$OnGroupExpandListener","android.widget.ExpandableListView$OnGroupExpandListener")
            .put("TabHost$TabContentFactory","android.widget.TabHost$TabContentFactory")
            .put("ProgressBar","android.widget.ProgressBar")
            .put("CursorAdapter","android.widget.CursorAdapter")
            .put("SlidingDrawer$OnDrawerScrollListener","android.widget.SlidingDrawer$OnDrawerScrollListener")
            .put("SpellCheckerInfo","android.view.textservice.SpellCheckerInfo")
            .put("BaseAdapter","android.widget.BaseAdapter")
            .put("SearchEvent","android.view.SearchEvent")
            .put("ZoomButton","android.widget.ZoomButton")
            .put("CollapsibleActionView","android.view.CollapsibleActionView")
            .put("CursorTreeAdapter","android.widget.CursorTreeAdapter")
            .put("TouchDelegate","android.view.TouchDelegate")
            .put("TextureView$SurfaceTextureListener","android.view.TextureView$SurfaceTextureListener")
            .put("ImageSwitcher","android.widget.ImageSwitcher")
            .put("InputMethodSubtype$InputMethodSubtypeBuilder","android.view.inputmethod.InputMethodSubtype$InputMethodSubtypeBuilder")
            .put("RemoteViews","android.widget.RemoteViews")
            .put("AccessibilityEventSource","android.view.accessibility.AccessibilityEventSource")
            .put("SubMenu","android.view.SubMenu")
            .put("CompoundButton$OnCheckedChangeListener","android.widget.CompoundButton$OnCheckedChangeListener")
            .put("SlidingDrawer","android.widget.SlidingDrawer")
            .put("WindowAnimationFrameStats","android.view.WindowAnimationFrameStats")
            .put("MultiAutoCompleteTextView$CommaTokenizer","android.widget.MultiAutoCompleteTextView$CommaTokenizer")
            .put("GridView","android.widget.GridView")
            .put("InputConnectionWrapper","android.view.inputmethod.InputConnectionWrapper")
            .put("OrientationEventListener","android.view.OrientationEventListener")
            .put("AbsListView$SelectionBoundsAdjuster","android.widget.AbsListView$SelectionBoundsAdjuster")
            .put("AccessibilityNodeInfo$CollectionItemInfo","android.view.accessibility.AccessibilityNodeInfo$CollectionItemInfo")
            .put("NumberPicker","android.widget.NumberPicker")
            .put("SearchView","android.widget.SearchView")
            .put("Choreographer$FrameCallback","android.view.Choreographer$FrameCallback")
            .put("PixelCopy","android.view.PixelCopy")
            .put("Switch","android.widget.Switch")
            .put("View$OnScrollChangeListener","android.view.View$OnScrollChangeListener")
            .put("GridLayout$Alignment","android.widget.GridLayout$Alignment")
            .put("ViewDebug$ExportedProperty","android.view.ViewDebug$ExportedProperty")
            .put("AdapterView$OnItemLongClickListener","android.widget.AdapterView$OnItemLongClickListener")
            .put("AbsListView$LayoutParams","android.widget.AbsListView$LayoutParams")
            .put("SlidingDrawer$OnDrawerCloseListener","android.widget.SlidingDrawer$OnDrawerCloseListener")
            .put("ActionMenuView","android.widget.ActionMenuView")
            .put("ViewTreeObserver$OnWindowFocusChangeListener","android.view.ViewTreeObserver$OnWindowFocusChangeListener")
            .put("View$OnSystemUiVisibilityChangeListener","android.view.View$OnSystemUiVisibilityChangeListener")
            .put("ExtractedText","android.view.inputmethod.ExtractedText")
            .put("PopupMenu$OnDismissListener","android.widget.PopupMenu$OnDismissListener")
            .put("ActionProvider","android.view.ActionProvider")
            .put("FocusFinder","android.view.FocusFinder")
            .put("CursorAnchorInfo$Builder","android.view.inputmethod.CursorAnchorInfo$Builder")
            .put("MenuItem$OnActionExpandListener","android.view.MenuItem$OnActionExpandListener")
            .put("WindowInsets","android.view.WindowInsets")
            .put("Scroller","android.widget.Scroller")
            .put("ViewParent","android.view.ViewParent")
            .put("SimpleCursorAdapter$ViewBinder","android.widget.SimpleCursorAdapter$ViewBinder")
            .put("AccessibilityNodeProvider","android.view.accessibility.AccessibilityNodeProvider")
            .put("ViewDebug$RecyclerTraceType","android.view.ViewDebug$RecyclerTraceType")
            .put("ViewAnimator","android.widget.ViewAnimator")
            .put("PopupWindow","android.widget.PopupWindow")
            .put("RemoteViewsService$RemoteViewsFactory","android.widget.RemoteViewsService$RemoteViewsFactory")
            .put("AnticipateOvershootInterpolator","android.view.animation.AnticipateOvershootInterpolator")
            .put("ScaleGestureDetector$SimpleOnScaleGestureListener","android.view.ScaleGestureDetector$SimpleOnScaleGestureListener")
            .put("GridLayoutAnimationController","android.view.animation.GridLayoutAnimationController")
            .put("InputMethodManager","android.view.inputmethod.InputMethodManager")
            .put("ListView","android.widget.ListView")
            .put("Button","android.widget.Button")
            .put("ActionProvider$VisibilityListener","android.view.ActionProvider$VisibilityListener")
            .put("ScaleGestureDetector","android.view.ScaleGestureDetector")
            .put("ViewOverlay","android.view.ViewOverlay")
            .put("AccessibilityWindowInfo","android.view.accessibility.AccessibilityWindowInfo")
            .put("NumberPicker$OnValueChangeListener","android.widget.NumberPicker$OnValueChangeListener")
            .put("GestureDetector$SimpleOnGestureListener","android.view.GestureDetector$SimpleOnGestureListener")
            .put("ViewDebug$CapturedViewProperty","android.view.ViewDebug$CapturedViewProperty")
            .put("ActionMenuView$OnMenuItemClickListener","android.widget.ActionMenuView$OnMenuItemClickListener")
            .put("VideoView","android.widget.VideoView")
            .put("SurfaceHolder$Callback2","android.view.SurfaceHolder$Callback2")
            .put("ShareActionProvider$OnShareTargetSelectedListener","android.widget.ShareActionProvider$OnShareTargetSelectedListener")
            .put("DragEvent","android.view.DragEvent")
            .put("RelativeLayout","android.widget.RelativeLayout")
            .put("KeyCharacterMap$KeyData","android.view.KeyCharacterMap$KeyData")
            .put("ViewTreeObserver$OnScrollChangedListener","android.view.ViewTreeObserver$OnScrollChangedListener")
            .put("ToggleButton","android.widget.ToggleButton")
            .put("RadioGroup","android.widget.RadioGroup")
            .put("GestureDetector$OnGestureListener","android.view.GestureDetector$OnGestureListener")
            .put("LinearLayout","android.widget.LinearLayout")
            .put("TextView$BufferType","android.widget.TextView$BufferType")
            .put("View$AccessibilityDelegate","android.view.View$AccessibilityDelegate")
            .put("InputMethodSession$EventCallback","android.view.inputmethod.InputMethodSession$EventCallback")
            .put("Gravity","android.view.Gravity")
            .put("ViewSwitcher","android.widget.ViewSwitcher")
            .put("FilterQueryProvider","android.widget.FilterQueryProvider")
            .put("TimePicker","android.widget.TimePicker")
            .put("HorizontalScrollView","android.widget.HorizontalScrollView")
            .put("KeyCharacterMap$UnavailableException","android.view.KeyCharacterMap$UnavailableException")
            .put("MenuItem$OnMenuItemClickListener","android.view.MenuItem$OnMenuItemClickListener")
            .put("ContextThemeWrapper","android.view.ContextThemeWrapper")
            .put("CompletionInfo","android.view.inputmethod.CompletionInfo")
            .put("KeyboardShortcutGroup","android.view.KeyboardShortcutGroup")
            .put("InflateException","android.view.InflateException")
            .put("PixelCopy$OnPixelCopyFinishedListener","android.view.PixelCopy$OnPixelCopyFinishedListener")
            .put("GridLayoutAnimationController$AnimationParameters","android.view.animation.GridLayoutAnimationController$AnimationParameters")
            .put("AnimationSet","android.view.animation.AnimationSet")
            .put("SurfaceHolder$BadSurfaceTypeException","android.view.SurfaceHolder$BadSurfaceTypeException")
            .put("Filter$FilterListener","android.widget.Filter$FilterListener")
            .put("RadioGroup$LayoutParams","android.widget.RadioGroup$LayoutParams")
            .put("AccessibilityNodeInfo$AccessibilityAction","android.view.accessibility.AccessibilityNodeInfo$AccessibilityAction")
            .put("AbsoluteLayout","android.widget.AbsoluteLayout")
            .put("AutoCompleteTextView$OnDismissListener","android.widget.AutoCompleteTextView$OnDismissListener")
            .put("ListAdapter","android.widget.ListAdapter")
            .put("DragAndDropPermissions","android.view.DragAndDropPermissions")
            .put("AdapterView$OnItemSelectedListener","android.widget.AdapterView$OnItemSelectedListener")
            .put("ZoomButtonsController$OnZoomListener","android.widget.ZoomButtonsController$OnZoomListener")
            .put("SimpleAdapter","android.widget.SimpleAdapter")
            .put("LayoutInflater$Factory","android.view.LayoutInflater$Factory")
            .put("LayoutInflater","android.view.LayoutInflater")
            .put("ViewStub$OnInflateListener","android.view.ViewStub$OnInflateListener")
            .put("ViewConfiguration","android.view.ViewConfiguration")
            .put("InputContentInfo","android.view.inputmethod.InputContentInfo")
            .put("ThemedSpinnerAdapter","android.widget.ThemedSpinnerAdapter")
            .put("AdapterView$OnItemClickListener","android.widget.AdapterView$OnItemClickListener")
            .put("View$OnFocusChangeListener","android.view.View$OnFocusChangeListener")
            .put("ViewTreeObserver$OnGlobalLayoutListener","android.view.ViewTreeObserver$OnGlobalLayoutListener")
            .put("ShareActionProvider","android.widget.ShareActionProvider")
            .put("ViewOutlineProvider","android.view.ViewOutlineProvider")
            .put("Window","android.view.Window")
            .put("TextSwitcher","android.widget.TextSwitcher")
            .put("ViewTreeObserver$OnDrawListener","android.view.ViewTreeObserver$OnDrawListener")
            .put("WindowManager$BadTokenException","android.view.WindowManager$BadTokenException")
            .put("ListPopupWindow","android.widget.ListPopupWindow")
            .put("InputMethodSubtype","android.view.inputmethod.InputMethodSubtype")
            .put("ExtractedTextRequest","android.view.inputmethod.ExtractedTextRequest")
            .put("ViewGroup$OnHierarchyChangeListener","android.view.ViewGroup$OnHierarchyChangeListener")
            .put("WindowId","android.view.WindowId")
            .put("Advanceable","android.widget.Advanceable")
            .put("ViewStructure","android.view.ViewStructure")
            .put("ExpandableListView$ExpandableListContextMenuInfo","android.widget.ExpandableListView$ExpandableListContextMenuInfo")
            .put("ScaleAnimation","android.view.animation.ScaleAnimation")
            .put("View$DragShadowBuilder","android.view.View$DragShadowBuilder")
            .put("InputMethod","android.view.inputmethod.InputMethod")
            .put("KeyEvent$DispatcherState","android.view.KeyEvent$DispatcherState")
            .put("EditorInfo","android.view.inputmethod.EditorInfo")
            .put("ListView$FixedViewInfo","android.widget.ListView$FixedViewInfo")
            .put("ViewTreeObserver","android.view.ViewTreeObserver")
            .put("AbsoluteLayout$LayoutParams","android.widget.AbsoluteLayout$LayoutParams")
            .put("AdapterView$AdapterContextMenuInfo","android.widget.AdapterView$AdapterContextMenuInfo")
            .put("Chronometer","android.widget.Chronometer")
            .put("SimpleAdapter$ViewBinder","android.widget.SimpleAdapter$ViewBinder")
            .put("View$OnKeyListener","android.view.View$OnKeyListener")
            .put("VelocityTracker","android.view.VelocityTracker")
            .put("ResourceCursorTreeAdapter","android.widget.ResourceCursorTreeAdapter")
            .put("Animation","android.view.animation.Animation")
            .put("SurfaceHolder","android.view.SurfaceHolder")
            .put("WrapperListAdapter","android.widget.WrapperListAdapter")
            .put("InputConnection","android.view.inputmethod.InputConnection")
            .put("AutoCompleteTextView","android.widget.AutoCompleteTextView")
            .put("ImageView","android.widget.ImageView")
            .put("Choreographer","android.view.Choreographer")
            .put("TabHost$TabSpec","android.widget.TabHost$TabSpec")
            .put("NumberPicker$Formatter","android.widget.NumberPicker$Formatter")
            .put("SimpleCursorTreeAdapter$ViewBinder","android.widget.SimpleCursorTreeAdapter$ViewBinder")
            .put("DecelerateInterpolator","android.view.animation.DecelerateInterpolator")
            .put("ViewGroupOverlay","android.view.ViewGroupOverlay")
            .put("Gallery$LayoutParams","android.widget.Gallery$LayoutParams")
            .put("AccessibilityEvent","android.view.accessibility.AccessibilityEvent")
            .put("ViewGroup","android.view.ViewGroup")
            .put("Filter$FilterResults","android.widget.Filter$FilterResults")
            .put("RemoteViews$ActionException","android.widget.RemoteViews$ActionException")
            .put("AbsListView$RecyclerListener","android.widget.AbsListView$RecyclerListener")
            .put("DatePicker","android.widget.DatePicker")
            .put("ViewDebug$FlagToString","android.view.ViewDebug$FlagToString")
            .put("TwoLineListItem","android.widget.TwoLineListItem")
            .put("SpellCheckerSubtype","android.view.textservice.SpellCheckerSubtype")
            .put("CheckedTextView","android.widget.CheckedTextView")
            .put("FrameLayout","android.widget.FrameLayout")
            .put("ViewFlipper","android.widget.ViewFlipper")
            .put("OvershootInterpolator","android.view.animation.OvershootInterpolator")
            .put("Filter","android.widget.Filter")
            .put("MultiAutoCompleteTextView","android.widget.MultiAutoCompleteTextView")
            .put("FrameMetrics","android.view.FrameMetrics")
            .put("SurfaceHolder$Callback","android.view.SurfaceHolder$Callback")
            .put("OverScroller","android.widget.OverScroller")
            .put("BounceInterpolator","android.view.animation.BounceInterpolator")
            .put("Surface$OutOfResourcesException","android.view.Surface$OutOfResourcesException")
            .put("ImageButton","android.widget.ImageButton")
            .put("AlphabetIndexer","android.widget.AlphabetIndexer")
            .put("RelativeLayout$LayoutParams","android.widget.RelativeLayout$LayoutParams")
            .put("KeyboardShortcutInfo","android.view.KeyboardShortcutInfo")
            .put("SentenceSuggestionsInfo","android.view.textservice.SentenceSuggestionsInfo")
            .put("Filterable","android.widget.Filterable")
            .put("View$OnApplyWindowInsetsListener","android.view.View$OnApplyWindowInsetsListener")
            .put("TextView$OnEditorActionListener","android.widget.TextView$OnEditorActionListener")
            .put("InputQueue","android.view.InputQueue")
            .put("SeekBar","android.widget.SeekBar")
            .put("View$OnGenericMotionListener","android.view.View$OnGenericMotionListener")
            .put("ViewGroup$MarginLayoutParams","android.view.ViewGroup$MarginLayoutParams")
            .put("CompoundButton","android.widget.CompoundButton")
            .put("Display$Mode","android.view.Display$Mode")
            .put("FrameLayout$LayoutParams","android.widget.FrameLayout$LayoutParams")
            .put("TableRow","android.widget.TableRow")
            .put("CalendarView$OnDateChangeListener","android.widget.CalendarView$OnDateChangeListener")
            .put("AbsListView$OnScrollListener","android.widget.AbsListView$OnScrollListener")
            .put("AccessibilityRecord","android.view.accessibility.AccessibilityRecord")
            .put("View$OnCreateContextMenuListener","android.view.View$OnCreateContextMenuListener")
            .put("SearchView$OnQueryTextListener","android.widget.SearchView$OnQueryTextListener")
            .put("ContextMenu$ContextMenuInfo","android.view.ContextMenu$ContextMenuInfo")
            .put("SuggestionsInfo","android.view.textservice.SuggestionsInfo")
            .put("TranslateAnimation","android.view.animation.TranslateAnimation")
            .put("PopupMenu","android.widget.PopupMenu")
            .put("ActionMode$Callback2","android.view.ActionMode$Callback2")
            .put("TextureView","android.view.TextureView")
            .put("BaseInputConnection","android.view.inputmethod.BaseInputConnection")
            .put("InputMethodInfo","android.view.inputmethod.InputMethodInfo")
            .put("GridLayout","android.widget.GridLayout")
            .put("MenuItem","android.view.MenuItem")
            .put("SpellCheckerSession$SpellCheckerSessionListener","android.view.textservice.SpellCheckerSession$SpellCheckerSessionListener")
            .put("SearchView$OnCloseListener","android.widget.SearchView$OnCloseListener")
            .put("AccessibilityNodeInfo","android.view.accessibility.AccessibilityNodeInfo")
            .put("Toast","android.widget.Toast")
            .put("SimpleCursorAdapter$CursorToStringConverter","android.widget.SimpleCursorAdapter$CursorToStringConverter")
            .put("AccessibilityNodeInfo$CollectionInfo","android.view.accessibility.AccessibilityNodeInfo$CollectionInfo")
            .put("CheckBox","android.widget.CheckBox")
            .put("Toolbar$OnMenuItemClickListener","android.widget.Toolbar$OnMenuItemClickListener")
            .put("TextInfo","android.view.textservice.TextInfo")
            .put("Window$Callback","android.view.Window$Callback")
            .put("ExpandableListView$OnGroupClickListener","android.widget.ExpandableListView$OnGroupClickListener")
            .put("AbsListView","android.widget.AbsListView")
            .put("View$BaseSavedState","android.view.View$BaseSavedState")
            .put("WindowManager","android.view.WindowManager")
            .put("InputBinding","android.view.inputmethod.InputBinding")
            .put("QuickContactBadge","android.widget.QuickContactBadge")
            .put("ImageView$ScaleType","android.widget.ImageView$ScaleType")
            .put("View","android.view.View")
            .put("RotateAnimation","android.view.animation.RotateAnimation")
            .put("DialerFilter","android.widget.DialerFilter")
            .put("RemoteViewsService","android.widget.RemoteViewsService")
            .put("CalendarView","android.widget.CalendarView")
            .put("Transformation","android.view.animation.Transformation")
            .put("Animation$Description","android.view.animation.Animation$Description")
            .put("Surface","android.view.Surface")
            .put("CaptioningManager$CaptioningChangeListener","android.view.accessibility.CaptioningManager$CaptioningChangeListener")
            .put("ViewTreeObserver$OnWindowAttachListener","android.view.ViewTreeObserver$OnWindowAttachListener")
            .put("Display$HdrCapabilities","android.view.Display$HdrCapabilities")
            .put("ViewPropertyAnimator","android.view.ViewPropertyAnimator")
            .put("Interpolator","android.view.animation.Interpolator")
            .put("KeyEvent$Callback","android.view.KeyEvent$Callback")
            .put("ViewDebug$IntToString","android.view.ViewDebug$IntToString")
            .put("PopupWindow$OnDismissListener","android.widget.PopupWindow$OnDismissListener")
            .build();
}
