const versionsJsonURL = "https://raw.githubusercontent.com/PuzzlesHQ/puzzle-loader-core/refs/heads/versioning/versions.json";

export function getVersionDataAsync() {
    const dataGet = async () => {
        try {
            const response = await fetch(versionsJsonURL);
            if (!response.ok) {
                throw new Error(`Response status: ${response.status}`);
            }
            const json = await response.json();
            const data = json;
            return data;
        }
        catch (error) {
            console.error(error);
            return undefined;
        }
    };
    return dataGet();
}

export function getVersionDataImmediate() {
    try {
        const xhr = new XMLHttpRequest();
        xhr.overrideMimeType('text/plain; charset=x-user-defined');
        xhr.open('GET', versionsJsonURL, false);
        xhr.send();
        const uint8 = Uint8Array.from(xhr.response, (c) => c.charCodeAt(0));
        const text = new TextDecoder().decode(uint8);
        const json = JSON.parse(text);
        return json;
    }
    catch (e) {
        return undefined;
    }
}
