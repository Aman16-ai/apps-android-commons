package fr.free.nrw.commons.customselector.ui.selector

import android.net.Uri
import android.os.Bundle
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Custom Selector Activity Test
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class CustomSelectorActivityTest {

    private lateinit var activity: CustomSelectorActivity

    /**
     * Set up the tests.
     */
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        activity = Robolectric.buildActivity(CustomSelectorActivity::class.java)
            .get()
        val onCreate = activity.javaClass.getDeclaredMethod("onCreate", Bundle::class.java)
        onCreate.isAccessible = true
        onCreate.invoke(activity, null)
    }

    /**
     * Test activity not null.
     */
    @Test
    @Throws(Exception::class)
    fun testActivityNotNull() {
        assertNotNull(activity)
    }

    /**
     * Test changeTitle function.
     */
    @Test
    @Throws(Exception::class)
    fun testChangeTitle() {
        val func = activity.javaClass.getDeclaredMethod("changeTitle", String::class.java)
        func.isAccessible = true
        func.invoke(activity, "test")
    }

    /**
     * Test onFolderClick function.
     */
    @Test
    @Throws(Exception::class)
    fun testOnFolderClick() {
        activity.onFolderClick(Folder(1, "test", arrayListOf()));
    }

    /**
     * Test selectedImagesChanged function.
     */
    @Test
    @Throws(Exception::class)
    fun testOnSelectedImagesChanged() {
        activity.onSelectedImagesChanged(ArrayList())
    }

    /**
     * Test onDone function.
     */
    @Test
    @Throws(Exception::class)
    fun testOnDone() {
        activity.onDone()
        activity.onSelectedImagesChanged(ArrayList(arrayListOf(Image(1, "test", Uri.parse("test"), "test", 1))));
        activity.onDone()
    }

    /**
     * Test onBackPressed Function.
     */
    @Test
    @Throws(Exception::class)
    fun testOnBackPressed() {
        activity.onBackPressed()
    }
}