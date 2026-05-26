Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile('C:\Users\Arthur Esteves\Desktop\IBF\IBF_APP-1\app\src\main\res\drawable\care_logo.jpg')
$bmp = New-Object System.Drawing.Bitmap($img)
for ($y = 0; $y -lt $bmp.Height; $y++) {
    for ($x = 0; $x -lt $bmp.Width; $x++) {
        $c = $bmp.GetPixel($x, $y)
        $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255 - $c.R, 255 - $c.G, 255 - $c.B))
    }
}
$bmp.Save('C:\Users\Arthur Esteves\Desktop\IBF\IBF_APP-1\app\src\main\res\drawable\care_logo_dark.jpg')
$img.Dispose()
$bmp.Dispose()
