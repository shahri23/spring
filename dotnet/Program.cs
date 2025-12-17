var builder = WebApplication.CreateBuilder(args);

var app = builder.Build();

app.MapGet("/", () => "Hello World from ASP.NET Core with New Relic on Alpine!");

app.MapGet("/health", () => new { status = "healthy", timestamp = DateTime.UtcNow });

app.Run();
