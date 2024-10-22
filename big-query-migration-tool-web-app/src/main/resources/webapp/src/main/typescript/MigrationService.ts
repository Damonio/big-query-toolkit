export default class MigrationService {


    static migrate() {
        const migrationDetails = new FormData();
        migrationDetails.append('migrationRequest', this.getBody());
        migrationDetails.append('email', 'john@example.com');

        return new Promise(async (resolve) => {
            const response = await fetch('http://localhost:8080/migrate', {
                method: 'POST',
                headers: headers(),
                body: migrationDetails
            });
            resolve(await response.text());
        })
    }

    private static getBody() {
        return JSON.stringify({
            projectId: 'my-project-id',
            datasetId: 'my-dataset-id',
            credentials: 'my-credentials',
            replacements: {}
        });
    }


}

function headers() {
    return {
        Accept: 'application/json', 'Content-Type': 'multipart/form-data'
    };
}

/**
 *
 * @Data
 * @NoArgsConstructor
 * @AllArgsConstructor
 * class MigrationRequest {
 *     private String projectId;
 *     private String datasetId;
 *     private String credentials;
 *     private Map<String, String> replacements = new HashMap<>();
 * }
 */