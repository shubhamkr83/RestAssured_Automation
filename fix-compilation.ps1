# Fix all compilation errors at once
$files = @(
    @{path='src\test\java\com\automation\tests\buyerapp\HomePage\ContinueYourJourneyTest.java'; oldClass='Homepage_Continue_your_Journey'; newClass='ContinueYourJourneyTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\HomePage\BannersTest.java'; oldClass='Homepage_Banners'; newClass='BannersTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\HomePage\TrendingTest.java'; oldClass='Homepage_Trending'; newClass='TrendingTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\HomePage\FeaturedCollectionTest.java'; oldClass='Homepage_Featured_Collection'; newClass='FeaturedCollectionTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\HomePage\NewThisWeekTest.java'; oldClass='Homepage_New_This_Week'; newClass='NewThisWeekTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\HomePage\CatalogFeedTest.java'; oldClass='Homepage_Catalog_Feed'; newClass='CatalogFeedTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\HomePage\FeedFilterTest.java'; oldClass='Homepage_Feed_filter'; newClass='FeedFilterTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\SearchPage\SearchProductTest.java'; oldClass='Search_Search_Product'; newClass='SearchProductTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\SearchPage\SearchRecommendedChipsTest.java'; oldClass='Search_Recommended_Chips'; newClass='SearchRecommendedChipsTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\SearchPage\SearchRecommendChipSelectTest.java'; oldClass='Search_Recommend_Chip_Select'; newClass='SearchRecommendChipSelectTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\ProductPage\ProductSimilarCollectionTest.java'; oldClass='Product_Page_Product_Similar_Collection'; newClass='ProductSimilarCollectionTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\PDP\PdpSimilarTest.java'; oldClass='PDP_PDP_Similar'; newClass='PdpSimilarTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\ProfilePage\WatchedVideosTest.java'; oldClass='Profile_Watched_Videos'; newClass='WatchedVideosTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\ProfilePage\AppUpdateTest.java'; oldClass='Profile_App_update'; newClass='AppUpdateTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\ProfilePage\UserProfileTest.java'; oldClass='Profile_User'; newClass='UserProfileTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\ProfilePage\AuthValidateTest.java'; oldClass='Profile_Auth_Validate'; newClass='AuthValidateTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\SuitableForConfigTest.java'; oldClass='Suitable_for_Config'; newClass='SuitableForConfigTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\CatalogByIdTest.java'; oldClass='Catalog'; newClass='CatalogByIdTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\CollectionListing\TopCollectionTest.java'; oldClass='Collection_Tab_Top_Collection'; newClass='TopCollectionTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\CollectionListing\SimilarCollection\SimilarCollectionForSareeTest.java'; oldClass='Collection_Tab_Collection_Similar_Collection_Saree'; newClass='SimilarCollectionForSareeTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\CollectionListing\SimilarCollection\SimilarCollectionForReadymadeTest.java'; oldClass='Collection_Tab_Collection_Similar_Collection_Readymade'; newClass='SimilarCollectionForReadymadeTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\CollectionListing\AllCollection\CollectionAllForSareeTest.java'; oldClass='Collection_Tab_Collection_All_for_Saree'; newClass='CollectionAllForSareeTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\CollectionListing\AllCollection\CollectionAllForReadymadeTest.java'; oldClass='Collection_Tab_Collection_All_for_Readymade'; newClass='CollectionAllForReadymadeTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\CollectionListing\AllCollectionCounts\CollectionCountsForSareeTest.java'; oldClass='Collection_Counts_for_Saree'; newClass='CollectionCountsForSareeTest'},
    @{path='src\test\java\com\automation\tests\buyerapp\CollectionListing\AllCollectionCounts\CollectionCountsForReadymadeTest.java'; oldClass='Collection_Counts_for_Readymade'; newClass='CollectionCountsForReadymadeTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogSearch\AllCatalogTest.java'; oldClass='Catalog_Search_All_Catalog'; newClass='AllCatalogTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogSearch\CatalogEditTest.java'; oldClass='Catalog_Search_Catalog_Edit'; newClass='CatalogEditTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogSearch\CatalogDeleteTest.java'; oldClass='Catalog_Search_Catalog_Delete'; newClass='CatalogDeleteTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogSearch\SearchWithProductFilterTest.java'; oldClass='Catalog_Search_with_Product_Filter'; newClass='SearchWithProductFilterTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogSearch\SearchWithSellerFilterTest.java'; oldClass='Catalog_Search_with_Seller_Filter'; newClass='SearchWithSellerFilterTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogSearch\SearchWithCatalogIdFilterTest.java'; oldClass='Catalog_Search_with_Catalog_ID_Filter'; newClass='SearchWithCatalogIdFilterTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogTagPipeline\CatalogAssignToEditor\AssignToEditorTest.java'; oldClass='Catalog_Assign_Assign_to_Editor'; newClass='AssignToEditorTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogTagPipeline\CatalogAssignToEditor\AllCatalogUploadedTest.java'; oldClass='Catalog_Assign_All_catalog_uploaded'; newClass='AllCatalogUploadedTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogTagPipeline\CatalogAssignToEditor\CatalogUploadedSearchBySellerFilterTest.java'; oldClass='Catalog_Assign_Catalog_Uploaded_Search_by_Seller_Filter'; newClass='CatalogUploadedSearchBySellerFilterTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogTagPipeline\CatalogEditor\FetchCatalogUploadedTest.java'; oldClass='Catalog_Editor_Fetch_Catalog_Uploaded'; newClass='FetchCatalogUploadedTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogTagPipeline\CatalogEditor\CatalogTagTest.java'; oldClass='Catalog_Editor_Catalog_Tag'; newClass='CatalogTagTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogTagPipeline\CatalogEditor\CatalogEditorEditTest.java'; oldClass='Catalog_Editor_Catalog_Edit'; newClass='CatalogEditorEditTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogTagPipeline\CatalogEditor\CatalogSkipTest.java'; oldClass='Catalog_Editor_Catalog_Skip'; newClass='CatalogSkipTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogTagPipeline\CatalogEditor\CatalogsGroupTest.java'; oldClass='Catalog_Editor_Catalogs_Group'; newClass='CatalogsGroupTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogTagPipeline\CatalogEditor\AllCatalogsAssignedTest.java'; oldClass='Catalog_Editor_All_Catalogs_Assigned'; newClass='AllCatalogsAssignedTest'},
    @{path='src\test\java\com\automation\tests\bomb\CatalogTagPipeline\CatalogEditor\MarkAsDoneTest.java'; oldClass='MarkAsDone'; newClass='MarkAsDoneTest'},
    @{path='src\test\java\com\automation\tests\bomb\VideoTaggingPipeline\VideoUploadAndAssignToEditor\UploadThumbnailVideoTest.java'; oldClass='Video_Upload_Thumbnail_Video'; newClass='UploadThumbnailVideoTest'},
    @{path='src\test\java\com\automation\tests\bomb\VideoTaggingPipeline\VideoUploadAndAssignToEditor\VideoUploadMarkAsDoneTest.java'; oldClass='Video_Upload_mark_as_done'; newClass='VideoUploadMarkAsDoneTest'},
    @{path='src\test\java\com\automation\tests\bomb\VideoTaggingPipeline\VideoUploadAndAssignToEditor\AllVideoAssignForUploadingThumbnailVideoTest.java'; oldClass='Video_Upload_All_Video_Assign_for_uploading_Thumbnail_Video'; newClass='AllVideoAssignForUploadingThumbnailVideoTest'},
    @{path='src\test\java\com\automation\tests\bomb\VideoTaggingPipeline\VideoTagging\VideoTaggingEditButtonTest.java'; oldClass='Video_Tagging_Edit_Button'; newClass='VideoTaggingEditButtonTest'}
)

foreach ($file in $files) {
    Write-Host "Fixing $($file.path)..."
    $content = Get-Content $file.path -Raw
    $content = $content -replace 'import static com.automation.tests.buyerapp.Login.login.VariableManager.getBuyerAppToken();', 'import com.automation.utils.VariableManager;'
    $content = $content -replace "public class $($file.oldClass)", "public class $($file.newClass)"
    Set-Content -Path $file.path -Value $content -NoNewline
}

Write-Host "All files fixed!"
