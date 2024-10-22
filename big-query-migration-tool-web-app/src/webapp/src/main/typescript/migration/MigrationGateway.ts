import getUrl from "../api/Api";

export class MigrationStatus {
    message: string;
    exception: string;

    constructor(status: string = '', exception: string = '') {
        this.message = status;
        this.exception = exception;
    }}

export default class MigrationGateway {

    static migrate(migrationDetails: FormData): Promise<MigrationStatus> {
        return new Promise(async (resolve) => {
            const response = await fetch(`${getUrl()}/migrate`, {
                method: 'POST',
                body: migrationDetails
            });
            if (!response.ok) {
                resolve(new MigrationStatus("Failure to migrate", "backend failure"));
            }
            resolve(await response.json());
        })
    }

}
