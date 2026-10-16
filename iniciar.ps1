$ErrorActionPreference = 'Stop'
$arquivoEnv = Join-Path $PSScriptRoot '.env'

if (Test-Path -LiteralPath $arquivoEnv) {
    foreach ($linha in Get-Content -LiteralPath $arquivoEnv -Encoding UTF8) {
        if ($linha -match '^\s*(DB_URL|DB_USER|DB_PASSWORD)\s*=(.*)$') {
            $nomeVariavel = $Matches[1]
            $valorVariavel = $Matches[2].Trim()
            if ($valorVariavel.Length -ge 2 -and
                (($valorVariavel.StartsWith('"') -and $valorVariavel.EndsWith('"')) -or
                 ($valorVariavel.StartsWith("'") -and $valorVariavel.EndsWith("'")))) {
                $valorVariavel = $valorVariavel.Substring(1, $valorVariavel.Length - 2)
            }
            if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($nomeVariavel, 'Process'))) {
                [Environment]::SetEnvironmentVariable($nomeVariavel, $valorVariavel, 'Process')
            }
        }
    }
}

foreach ($nomeVariavel in 'DB_URL', 'DB_USER', 'DB_PASSWORD') {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($nomeVariavel, 'Process'))) {
        throw "Configure $nomeVariavel no ambiente ou no arquivo .env local."
    }
}

if ($env:DB_URL -notmatch '^jdbc:mysql://') {
    throw 'DB_URL deve ser uma URL JDBC MySQL iniciada por jdbc:mysql://.'
}

$portas = [System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners()
if ($portas | Where-Object Port -EQ 8080) {
    throw 'A porta 8080 esta ocupada. Identifique a instancia em execucao antes de iniciar outra.'
}

Push-Location (Join-Path $PSScriptRoot 'financeiro/financeiro')
try {
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw 'A aplicacao nao iniciou corretamente. Confira o erro de inicializacao no terminal.'
    }
} finally {
    Pop-Location
}
