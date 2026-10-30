$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

Write-Host 'Configurar envio de verificaciones por Gmail (solo backend local).'
Write-Host 'Activa la verificacion en dos pasos y crea una clave de aplicacion:'
Write-Host 'https://myaccount.google.com/apppasswords'
$senderAddress = (Read-Host 'Tu direccion Gmail remitente').Trim()
if ($senderAddress -notmatch '^[A-Za-z0-9._%+-]+@gmail\.com$') {
    throw 'Introduce una direccion personal @gmail.com valida.'
}
do {
    $applicationSecret = Read-Host 'Clave de aplicacion de Google de 16 caracteres (no tu clave habitual)' -AsSecureString
    $secretPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($applicationSecret)
    try {
        $smtpSecret = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($secretPointer) -replace '\s', ''
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($secretPointer)
        $applicationSecret.Dispose()
    }
    $validSecret = $smtpSecret -cmatch '^[A-Za-z0-9]{16}$'
    if (-not $validSecret) {
        Write-Host 'El valor no tiene el formato esperado. Google entrega 16 caracteres, normalmente en 4 grupos de 4.' -ForegroundColor Yellow
        Write-Host 'Copia solo la clave generada en https://myaccount.google.com/apppasswords y vuelve a pegarla.'
        $smtpSecret = $null
    }
} until ($validSecret)
try {
    $smtpLines = @(
        'SMTP_HOST=smtp.gmail.com',
        'SMTP_PORT=587',
        "SMTP_USERNAME=$senderAddress",
        "SMTP_PASSWORD=$smtpSecret",
        'SMTP_AUTH=true',
        'SMTP_STARTTLS=true',
        "MAIL_FROM=$senderAddress"
    )
    $smtpFile = Join-Path $PSScriptRoot '.env.gmail'
    [IO.File]::WriteAllLines($smtpFile, $smtpLines, [Text.UTF8Encoding]::new($false))
    $smtpAcl = [Security.AccessControl.FileSecurity]::new()
    $smtpAcl.SetAccessRuleProtection($true, $false)
    $smtpOwner = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $smtpAcl.SetOwner($smtpOwner)
    $smtpAcl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($smtpOwner, 'FullControl', 'Allow'))
    Set-Acl -LiteralPath $smtpFile -AclObject $smtpAcl
} finally {
    $smtpSecret = $null
    $smtpLines = $null
}
docker compose --env-file .env.gmail up -d --no-deps --force-recreate backend
if ($LASTEXITCODE -ne 0) { throw 'No se pudo aplicar la configuracion al backend local.' }
Write-Host 'Gmail configurado. Espera a que arranque el backend y registra una cuenta en http://localhost:4200/registro'
Write-Host 'El enlace del correo apunta a localhost y debe abrirse en este equipo.'
