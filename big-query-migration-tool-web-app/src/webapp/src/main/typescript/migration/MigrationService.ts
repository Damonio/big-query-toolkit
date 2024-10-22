import getUrl from "../api/Api";

export default class MigrationService {

    static migrate(migrationDetails: FormData): Promise<boolean> {
        return new Promise(async (resolve) => {
            const response = await fetch(`${getUrl()}/migrate`, {
                method: 'POST',
                body: migrationDetails
            });
            resolve(response.ok);
        })
    }

}
