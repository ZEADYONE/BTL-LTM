param(
    [Parameter(Mandatory = $true)]
    [string]$DocPath,
    [Parameter(Mandatory = $true)]
    [string]$PdfPath,
    [switch]$SaveUpdatedFields
)

$ErrorActionPreference = 'Stop'
$word = $null
$document = $null

try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0
    Write-Output 'WORD_STARTED'

    $document = $word.Documents.Open($DocPath, $false, -not $SaveUpdatedFields)
    Write-Output 'DOC_OPENED'

    $word.Options.UpdateFieldsAtPrint = $true
    $document.ExportAsFixedFormat($PdfPath, 17)
    Write-Output 'PDF_EXPORTED'
    if ($SaveUpdatedFields) {
        $document.Save()
        Write-Output 'DOCX_SAVED'
    }
}
finally {
    if ($null -ne $document) {
        $document.Close($false)
    }
    if ($null -ne $word) {
        $word.Quit()
    }
    if ($null -ne $document) {
        [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($document)
    }
    if ($null -ne $word) {
        [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($word)
    }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
