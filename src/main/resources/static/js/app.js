const formatter = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
});

class App extends React.Component {
    constructor(props) {
        super(props);
        this.state = {openedWallet: null, wallets: []};
    }

    openInfo = (wallet) => {
        this.setState({
            openedWallet: wallet
        });
    };

    closeInfo = () => {
        this.setState({
            openedWallet: null
        });
    };

    logout = () => fetch("/api/logout").then(() => document.location.reload())

    componentDidMount() {
        fetch("/api/wallets")
            .then(res => res.json())
            .then(
                (result) => {
                    this.setState({
                        wallets: result
                    });
                },
                (error) => {
                    console.log(error)
                }
            )
    }

    render() {
        return (
            <main>
                <div className="form">
                    <div className="form-header">
                        Wallets
                    </div>
                    <div className="form-content">
                        {this.state.wallets.map(a =>
                            <div className="wallet" key={a.id}>
                                <div className="wallet-header">
                                    <div>
                                        {a.code}
                                    </div>
                                    <div>
                                        <button onClick={() => this.openInfo(a)}>Info</button>
                                    </div>
                                </div>
                                <div className="balance">
                                    {formatter.format(a.balance)}
                                </div>
                            </div>
                        )}
                        {
                            (!this.state.wallets || this.state.wallets.length === 0)
                            && <p style={{textAlign: 'center'}}>No wallets</p>
                        }
                    </div>
                    <div className="form-actions">
                        <button onClick={this.logout}>Logout</button>
                    </div>
                </div>

                <InfoDialog onClose={this.closeInfo} wallet={this.state.openedWallet}/>
            </main>
        )
    }
}

ReactDOM.render(
    <App/>,
    document.getElementById('app')
)
