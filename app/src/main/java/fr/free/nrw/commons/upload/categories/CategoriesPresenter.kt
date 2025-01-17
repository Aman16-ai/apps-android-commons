package fr.free.nrw.commons.upload.categories

import android.text.TextUtils
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryEditHelper
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.depicts.proxy
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * The presenter class for UploadCategoriesFragment
 */
@Singleton
class CategoriesPresenter @Inject constructor(
    private val repository: UploadRepository,
    @param:Named(CommonsApplicationModule.IO_THREAD) private val ioScheduler: Scheduler,
    @param:Named(CommonsApplicationModule.MAIN_THREAD) private val mainThreadScheduler: Scheduler
) : CategoriesContract.UserActionListener {

    companion object {
        private val DUMMY: CategoriesContract.View = proxy()
    }

    var view = DUMMY
    private val compositeDisposable = CompositeDisposable()
    private val searchTerms = PublishSubject.create<String>()
    /**
     * Current media
     */
    private var media: Media? = null

    /**
     * helper class for editing categories
     */
    @Inject
    lateinit var categoryEditHelper: CategoryEditHelper

    override fun onAttachView(view: CategoriesContract.View) {
        this.view = view
        compositeDisposable.add(
            searchTerms
                .observeOn(mainThreadScheduler)
                .doOnNext {
                    view.showProgress(true)
                    view.showError(null)
                    view.setCategories(null)
                }
                .switchMap(::searchResults)
                .map { repository.selectedCategories + it }
                .map { it.distinctBy { categoryItem -> categoryItem.name } }
                .observeOn(mainThreadScheduler)
                .subscribe(
                    {
                        view.setCategories(it)
                        view.showProgress(false)
                        if (it.isEmpty()) {
                            view.showError(R.string.no_categories_found)
                        }
                    },
                    Timber::e
                )
        )
    }

    /**
     * If media is null : Fetches categories from server according to the term
     * Else : Fetches existing categories by their name, fetches categories from server according
     * to the term and combines both in a list
     */
    private fun searchResults(term: String): Observable<List<CategoryItem>>? {
        if (media == null) {
            return repository.searchAll(term, getImageTitleList(), repository.selectedDepictions)
                .subscribeOn(ioScheduler)
                .map { it.filterNot { categoryItem -> repository.containsYear(categoryItem.name) } }
        } else {
            return Observable.zip(
                repository.getCategories(repository.selectedExistingCategories)
                    .map { list -> list.map {
                        CategoryItem(it.name, it.description, it.thumbnail, true)
                    }
                    },
                repository.searchAll(term, getImageTitleList(), repository.selectedDepictions),
                { it1, it2 ->
                    it1 + it2
                }
            )
                .subscribeOn(ioScheduler)
                .map { it.filterNot { categoryItem -> repository.containsYear(categoryItem.name) } }
                .map { it.filterNot { categoryItem -> categoryItem.thumbnail == "hidden" } }
        }
    }

    override fun onDetachView() {
        view = DUMMY
        compositeDisposable.clear()
    }

    /**
     * asks the repository to fetch categories for the query
     * @param query
     */
    override fun searchForCategories(query: String) {
        searchTerms.onNext(query)
    }

    /**
     * Returns image title list from UploadItem
     * @return
     */
    private fun getImageTitleList(): List<String> {
        return repository.uploads
            .map { it.uploadMediaDetails[0].captionText }
            .filterNot { TextUtils.isEmpty(it) }
    }

    /**
     * Verifies the number of categories selected, prompts the user if none selected
     */
    override fun verifyCategories() {
        val selectedCategories = repository.selectedCategories
        if (selectedCategories.isNotEmpty()) {
            repository.setSelectedCategories(selectedCategories.map { it.name })
            view.goToNextScreen()
        } else {
            view.showNoCategorySelected()
        }
    }

    /**
     * ask repository to handle category clicked
     *
     * @param categoryItem
     */
    override fun onCategoryItemClicked(categoryItem: CategoryItem) {
        repository.onCategoryClicked(categoryItem, media)
    }

    /**
     * Attaches view and media
     */
    override fun onAttachViewWithMedia(view: CategoriesContract.View, media: Media) {
        this.view = view
        this.media = media
        repository.selectedExistingCategories = view.existingCategories
        compositeDisposable.add(
            searchTerms
                .observeOn(mainThreadScheduler)
                .doOnNext {
                    view.showProgress(true)
                    view.showError(null)
                    view.setCategories(null)
                }
                .switchMap(::searchResults)
                .map { repository.selectedCategories + it }
                .map { it.distinctBy { categoryItem -> categoryItem.name } }
                .observeOn(mainThreadScheduler)
                .subscribe(
                    {
                        view.setCategories(it)
                        view.showProgress(false)
                        if (it.isEmpty()) {
                            view.showError(R.string.no_categories_found)
                        }
                    },
                    Timber::e
                )
        )
    }

    /**
     * Clears previous selections
     */
    override fun clearPreviousSelection() {
        repository.cleanup()
    }

    /**
     * Gets the selected categories and send them for posting to the server
     *
     * @param media media
     * @param wikiText current WikiText from server
     */
    override fun updateCategories(media: Media, wikiText: String) {
        if (repository.selectedCategories.isNotEmpty()
            || repository.selectedExistingCategories.size != view.existingCategories.size
        ) {
            val selectedCategories: MutableList<String> =
                (repository.selectedCategories.map { it.name }.toMutableList()
                        + repository.selectedExistingCategories).toMutableList()

            if (selectedCategories.isNotEmpty()) {
                view.showProgressDialog()
                compositeDisposable.add(
                    categoryEditHelper.makeCategoryEdit(view.fragmentContext, media,
                        selectedCategories, wikiText)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            Timber.d("Categories are added.")
                            media.addedCategories = selectedCategories
                            repository.cleanup()
                            view.dismissProgressDialog()
                            view.refreshCategories()
                            view.goBackToPreviousScreen()
                        })
                        {
                            Timber.e(
                                "Failed to update categories"
                            )
                        }
                )

            }
        } else {
            repository.cleanup()
            view.showNoCategorySelected()
        }
    }
}
