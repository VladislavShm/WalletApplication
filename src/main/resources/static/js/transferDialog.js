class TransferDialog extends React.Component {
    constructor(props) {
        super(props);
        this.state = {amount: null, toUser: null, toWallet: null, users: [], wallets: [], formDisabled: false};
    }

    componentDidMount() {
        fetch("/api/wallet/transfer/users")
            .then(res => res.json())
            .then(
                (result) => {
                    this.setState({
                        users: result
                    });
                },
                (error) => {
                    console.log(error)
                }
            )
    }

    handleAmount = amountEvent => {
        this.setState({
            amount: amountEvent.target.value
        });
    };

    handleToUser = toUserEvent => {
        let userId = toUserEvent.target.value;
        this.setState({
            toUser: userId,
            wallets: []
        });

        this.queryWallets(userId);
    };

    handleToWallet = toWalletEvent => {
        this.setState({
            toWallet: toWalletEvent.target.value
        });
    }

    onClose = () => {
        this.setState({amount: null, toUser: null, toWallet: null})
        this.props.onClose();
    }

    queryWallets = (userId) => {
        fetch("/api/wallet/transfer/wallets?" + new URLSearchParams({
            userId: userId,
            excludeWallet: this.props.walletId
        }))
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

    sendRequest = (event) => {
        event.preventDefault();

        let state = this.state;
        const requestOptions = {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                amount: state.amount,
                fromWalletId: this.props.walletId,
                toUser: state.toUser,
                toWalletId: state.toWallet
            })
        };

        this.setState({
            formDisabled: true
        })

        fetch('/api/wallet/transfer', requestOptions)
            .then(response => {
                if (response.ok) {
                    document.location.reload();
                } else {
                    console.log(response);
                }
            })
            .finally(() => {
                this.setState({
                    formDisabled: false
                })
            });
    }

    render() {
        return (
            <Modal show={this.props.show}>
                <div className="form">
                    <form onSubmit={this.sendRequest.bind(this)}>
                        <div className="form-header">
                            Transfer operation
                        </div>
                        <div className="form-content">
                            <div className="input-group">
                                <input value={this.state.amount} onChange={this.handleAmount} type="number" min="0.01"
                                       step="0.01" required placeholder={"Amount"} disabled={this.state.formDisabled}
                                       max={this.props.maxAmount}/>

                                <select value={this.state.toUser} onChange={this.handleToUser} required
                                        disabled={this.state.formDisabled}>
                                    <option value="" disabled selected hidden>Beneficiary</option>
                                    {this.state.users.map(u => <option value={u.id} key={u.id}>{u.username}</option>)}
                                </select>

                                <select value={this.state.toWallet} onChange={this.handleToWallet} required
                                        disabled={this.state.formDisabled}>
                                    <option value="" disabled selected hidden>Beneficiary wallet</option>
                                    {
                                        this.state.wallets
                                            .filter(a => a.id !== this.props.walletId)
                                            .map(a => <option value={a.id} key={a.id}>{a.code}</option>)
                                    }
                                </select>
                            </div>
                        </div>
                        <div className="form-actions">
                            <button type="submit" disabled={this.state.formDisabled}>
                                Submit
                            </button>
                            <button onClick={this.onClose} disabled={this.state.formDisabled}>
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>
            </Modal>
        )
    }
}
