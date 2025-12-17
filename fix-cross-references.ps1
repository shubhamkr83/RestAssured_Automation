# Fix cross-references to renamed classes
$replacements = @(
    @{file='src\test\java\com\automation\tests\buyerapp\HomePage\BannersTest.java'; old='import static com.automation.tests.buyerapp.HomePage.Homepage_Feed_Filter_Save.suitableFor;'; new='import static com.automation.tests.buyerapp.HomePage.FeedFilterSaveTest.suitableFor;'},
    @{file='src\test\java\com\automation\tests\buyerapp\HomePage\TrendingTest.java'; old='import static com.automation.tests.buyerapp.HomePage.Homepage_Feed_Filter_Save.suitableFor;'; new='import static com.automation.tests.buyerapp.HomePage.FeedFilterSaveTest.suitableFor;'},
    @{file='src\test\java\com\automation\tests\buyerapp\HomePage\FeaturedCollectionTest.java'; old='import static com.automation.tests.buyerapp.HomePage.Homepage_Feed_Filter_Save.suitableFor;'; new='import static com.automation.tests.buyerapp.HomePage.FeedFilterSaveTest.suitableFor;'},
    @{file='src\test\java\com\automation\tests\buyerapp\HomePage\NewThisWeekTest.java'; old='import static com.automation.tests.buyerapp.HomePage.Homepage_Feed_Filter_Save.suitableFor;'; new='import static com.automation.tests.buyerapp.HomePage.FeedFilterSaveTest.suitableFor;'},
    @{file='src\test\java\com\automation\tests\buyerapp\HomePage\CatalogFeedTest.java'; old='import static com.automation.tests.buyerapp.HomePage.Homepage_Feed_Filter_Save.suitableFor;'; new='import static com.automation.tests.buyerapp.HomePage.FeedFilterSaveTest.suitableFor;'}
)

foreach ($item in $replacements) {
    Write-Host "Fixing $($item.file)..."
    $content = Get-Content $item.file -Raw
    $content = $content -replace [regex]::Escape($item.old), $item.new
    Set-Content -Path $item.file -Value $content -NoNewline
}

# Fix SearchRecommendChipSelectTest references
Write-Host "Fixing SearchRecommendChipSelectTest..."
$file = 'src\test\java\com\automation\tests\buyerapp\SearchPage\SearchRecommendChipSelectTest.java'
$content = Get-Content $file -Raw
$content = $content -replace 'import static com.automation.tests.buyerapp.SearchPage.Search_Recommended_Chips.searchRecommend;', 'import static com.automation.tests.buyerapp.SearchPage.SearchRecommendedChipsTest.searchRecommend;'
$content = $content -replace 'import static com.automation.tests.buyerapp.SearchPage.Search_Recommended_Chips.searchRecommendId;', 'import static com.automation.tests.buyerapp.SearchPage.SearchRecommendedChipsTest.searchRecommendId;'
Set-Content -Path $file -Value $content -NoNewline

Write-Host "Cross-references fixed!"
